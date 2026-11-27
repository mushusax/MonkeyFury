package com.mushusax.monkeyfury.data

import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

// All timestamps are epoch milliseconds.

/** Which cards a study session shows. FSRS itself, which schedules each answer, runs on the server. */
object StudyConfig {
    /** How many never-studied cards are introduced per day, across all decks. */
    const val NEW_CARDS_PER_DAY = 20

    /**
     * Learning and relearning cards due within this long count as due, so a card answered Again
     * or Hard comes back in the same session instead of ending it early. They're shown after
     * every other due card, so they still wait out their step when there's more to study.
     */
    val LEARN_AHEAD_MS = 20.minutes.inWholeMilliseconds
}

@Serializable
data class Deck(
    val publicId: String,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

/** How many cards are due now, as a study session would show them. */
data class DueCounts(
    val byDeck: Map<String, Int> = emptyMap(),
    /** Across every deck. Not the sum of [byDeck], since the daily new-card limit is shared. */
    val total: Int = 0
)

/** What you write. A reversible note is studied as two cards, front→back and back→front. */
@Serializable
data class Note(
    val publicId: String,
    val deckId: String,
    val front: String,
    val back: String,
    val reversible: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class NoteParams(
    val front: String,
    val back: String,
    val reversible: Boolean = false
)

enum class CardDirection { FORWARD, REVERSE }

enum class CardState { NEW, LEARNING, REVIEW, RELEARNING }

/** FSRS ratings. [GOOD] is FSRS's plain "correct". */
enum class Rating {
    AGAIN, HARD, GOOD, EASY;

    /** The button text: the rating's name, e.g. "Again". */
    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * The fewest answers needed for a card in this state. New and (re)learning cards get Easy to
 * graduate now, Hard to repeat the step and Again to restart; day-based review gets Good, Hard and Again.
 */
val CardState.answerRatings: List<Rating>
    get() = when (this) {
        CardState.REVIEW -> listOf(Rating.GOOD, Rating.HARD, Rating.AGAIN)
        CardState.NEW, CardState.LEARNING, CardState.RELEARNING -> listOf(Rating.EASY, Rating.HARD, Rating.AGAIN)
    }

/** What you study: one direction of a note, with its FSRS state. */
@Serializable
data class Card(
    val publicId: String,
    val noteId: String,
    val direction: CardDirection,
    val state: CardState = CardState.NEW,
    /** Index into the learning or relearning steps while in those states. */
    val learningStep: Int = 0,
    /** FSRS stability in days; null until the first review. */
    val stability: Double? = null,
    /** FSRS difficulty, 1–10; null until the first review. */
    val difficulty: Double? = null,
    val due: Long,
    val lastReview: Long? = null,
    val updatedAt: Long
)

/** The body of GET /flashcards: every deck the user owns, with all of its notes and cards. */
@Serializable
data class FlashcardData(
    val decks: List<Deck>,
    val notes: List<Note>,
    val cards: List<Card>,
    /** Never-studied cards answered for the first time today, in the zone the app sent. */
    val newCardsToday: Int = 0,
    /** Answers per deck per day this month, in the zone the app sent; days without any are left out. */
    val reviewDays: List<ReviewDay> = emptyList()
)

/** How many answers were given to a deck's cards on [date] (yyyy-MM-dd). */
@Serializable
data class ReviewDay(
    val deckId: String,
    val date: String,
    val count: Int
)

/** The body of POST /cards/{id}/answer. */
@Serializable
data class AnswerParams(val rating: Rating)

/** What POST /decks/{id}/notes and PUT /notes/{id} return: the note and all of its cards after the change. */
@Serializable
data class NoteResult(
    val note: Note,
    val cards: List<Card>
)

/** A card ready to show, with its note's text resolved for the card's direction. */
data class StudyCard(
    val card: Card,
    val note: Note,
    val deckName: String
) {
    val prompt: String get() = if (card.direction == CardDirection.FORWARD) note.front else note.back
    val answer: String get() = if (card.direction == CardDirection.FORWARD) note.back else note.front
}
