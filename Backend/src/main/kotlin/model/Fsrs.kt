package com.mushusax.model

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

/** Hardcoded scheduling settings. */
object FsrsConfig {
    /** Target chance of remembering a card when it comes due. */
    const val DESIRED_RETENTION = 0.9

    /** Waits between correct answers before a new card graduates to day-based review. */
    val LEARNING_STEPS_MS = listOf(1.minutes, 10.minutes).map { it.inWholeMilliseconds }

    /** Wait after forgetting a reviewed card before it returns to day-based review. */
    val RELEARNING_STEPS_MS = listOf(10.minutes).map { it.inWholeMilliseconds }

    const val MAXIMUM_INTERVAL_DAYS = 36_500

    /** FSRS-5 default parameters. */
    val WEIGHTS = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046, 1.54575, 0.1192,
        1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315, 2.9898, 0.51655, 0.6621
    )
}

/** What a review log stores beyond the card's new state. */
data class ReviewOutcome(
    val stateBefore: CardState,
    val elapsedDays: Double,
    val scheduledDays: Double
)

private const val DAY_MS = 86_400_000.0
private const val DECAY = -0.5
private const val FACTOR = 19.0 / 81.0
private const val MIN_STABILITY = 0.01

/** FSRS-5 scheduling for all four ratings. No interval fuzz. */
class FsrsScheduler(
    private val w: DoubleArray = FsrsConfig.WEIGHTS,
    private val desiredRetention: Double = FsrsConfig.DESIRED_RETENTION,
    private val learningSteps: List<Long> = FsrsConfig.LEARNING_STEPS_MS,
    private val relearningSteps: List<Long> = FsrsConfig.RELEARNING_STEPS_MS,
    private val maximumIntervalDays: Int = FsrsConfig.MAXIMUM_INTERVAL_DAYS
) {

    /** Returns the card after answering it with [rating] at [now] (epoch ms), and what to log. */
    fun review(card: Card, rating: Rating, now: Long): Pair<Card, ReviewOutcome> {
        val g = rating.value
        val elapsedDays = card.lastReview?.let { max(0L, now - it) / DAY_MS } ?: 0.0

        val oldS = card.stability
        val oldD = card.difficulty
        val stability: Double
        val difficulty: Double
        if (card.state == CardState.NEW || oldS == null || oldD == null) {
            stability = initialStability(g)
            difficulty = initialDifficulty(g)
        } else {
            stability = when {
                elapsedDays < 1 -> shortTermStability(oldS, g)
                rating == Rating.AGAIN -> forgetStability(oldD, oldS, retrievability(elapsedDays, oldS))
                else -> recallStability(oldD, oldS, retrievability(elapsedDays, oldS), rating)
            }.coerceAtLeast(MIN_STABILITY)
            difficulty = nextDifficulty(oldD, g)
        }

        val next = when (card.state) {
            CardState.NEW -> step(learningSteps, 0, CardState.LEARNING, rating, stability)
            CardState.LEARNING -> step(learningSteps, card.learningStep, CardState.LEARNING, rating, stability)
            CardState.RELEARNING -> step(relearningSteps, card.learningStep, CardState.RELEARNING, rating, stability)
            CardState.REVIEW ->
                if (rating == Rating.AGAIN && relearningSteps.isNotEmpty()) {
                    Next(CardState.RELEARNING, 0, relearningSteps[0])
                } else {
                    review(stability)
                }
        }

        val updated = card.copy(
            state = next.state,
            learningStep = next.step,
            stability = stability,
            difficulty = difficulty,
            due = now + next.waitMs,
            lastReview = now,
            updatedAt = now
        )
        return updated to ReviewOutcome(card.state, elapsedDays, next.waitMs / DAY_MS)
    }

    private class Next(val state: CardState, val step: Int, val waitMs: Long)

    /**
     * Moves through learning or relearning steps: Again restarts them, Hard repeats the current one,
     * Good advances and graduates after the last, and Easy graduates at once.
     */
    private fun step(
        steps: List<Long>,
        currentStep: Int,
        stepState: CardState,
        rating: Rating,
        stability: Double
    ): Next {
        if (steps.isEmpty()) return review(stability)
        // A card saved under a longer step list may point past the end of this one
        val current = currentStep.coerceIn(0, steps.lastIndex)
        return when (rating) {
            Rating.AGAIN -> Next(stepState, 0, steps[0])
            Rating.HARD -> Next(stepState, current, hardStepWait(steps, current))
            Rating.GOOD -> {
                val nextStep = current + 1
                if (nextStep >= steps.size) review(stability) else Next(stepState, nextStep, steps[nextStep])
            }
            Rating.EASY -> review(stability)
        }
    }

    /**
     * Hard repeats the step with a slightly longer wait on the first step: halfway to the second
     * step, or 1.5× the step when it's the only one. Later steps repeat their own wait.
     */
    private fun hardStepWait(steps: List<Long>, current: Int): Long = when {
        current > 0 -> steps[current]
        steps.size == 1 -> (steps[0] * 1.5).toLong()
        else -> (steps[0] + steps[1]) / 2
    }

    private fun review(stability: Double) =
        Next(CardState.REVIEW, 0, (intervalDays(stability) * DAY_MS).toLong())

    private fun intervalDays(s: Double): Int =
        (s / FACTOR * (desiredRetention.pow(1 / DECAY) - 1)).roundToInt().coerceIn(1, maximumIntervalDays)

    private fun retrievability(elapsedDays: Double, s: Double) = (1 + FACTOR * elapsedDays / s).pow(DECAY)

    private fun initialStability(g: Int) = max(w[g - 1], MIN_STABILITY)

    private fun initialDifficulty(g: Int) = (w[4] - exp(w[5] * (g - 1)) + 1).coerceIn(1.0, 10.0)

    private fun nextDifficulty(d: Double, g: Int): Double {
        val delta = -w[6] * (g - 3)
        val damped = d + delta * (10 - d) / 9
        // Mean reversion toward the initial difficulty of an Easy answer
        return (w[7] * initialDifficulty(4) + (1 - w[7]) * damped).coerceIn(1.0, 10.0)
    }

    /** Reviews less than a day apart, e.g. learning steps. */
    private fun shortTermStability(s: Double, g: Int) = s * exp(w[17] * (g - 3 + w[18]))

    /** Hard shrinks the stability gain by w[15]; Easy grows it by w[16]. */
    private fun recallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.HARD) w[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) w[16] else 1.0
        return s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp(w[10] * (1 - r)) - 1) * hardPenalty * easyBonus)
    }

    private fun forgetStability(d: Double, s: Double, r: Double): Double {
        val longTerm = w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp(w[14] * (1 - r))
        return min(longTerm, s / exp(w[17] * w[18]))
    }
}
