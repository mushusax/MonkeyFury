package com.mushusax.monkeyfury.data

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

enum class ActivityKind(val label: String) {
    BOOLEAN("Yes/No"),
    NUMBER("Number"),
    DURATION("Duration")
}

/** A tracked activity. For DURATION, entry values and [goal] are whole minutes. */
@Serializable
data class Activity(
    val publicId: String,
    val name: String,
    val category: String,
    val kind: ActivityKind,
    val unit: String,
    val goal: Double? = null,
    /** True when a smaller value is the better day, e.g. alcoholic drinks; [goal] is then a limit. */
    val lowerIsBetter: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
data class ActivityParams(
    val name: String,
    val category: String,
    val kind: ActivityKind,
    val unit: String,
    val goal: Double?,
    val lowerIsBetter: Boolean,
    val sortOrder: Int = 0
)

/** One day's value for one activity. [date] is yyyy-MM-dd. */
@Serializable
data class ActivityEntry(
    val activityId: String,
    val date: String,
    val value: Double
)

@Serializable
data class EntryValueParams(val value: Double)

/** Formats minutes as h:mm, e.g. 90 -> "1:30". */
fun formatDuration(minutes: Double): String {
    val total = minutes.roundToInt()
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Parses "h:mm" (or "h:m") into minutes; a bare number is minutes, so "45" -> 45 and "1:30" -> 90.
 * Returns null for anything else, including minutes of 60 or more after a colon.
 */
fun parseDuration(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val parts = trimmed.split(":")
    return when (parts.size) {
        1 -> parts[0].toIntOrNull()?.takeIf { it >= 0 }?.toDouble()
        2 -> {
            val hours = parts[0].ifEmpty { "0" }.toIntOrNull() ?: return null
            val minutes = parts[1].toIntOrNull() ?: return null
            if (hours < 0 || minutes !in 0..59) null else (hours * 60 + minutes).toDouble()
        }
        else -> null
    }
}

/** Formats a number to at most 2 decimals without trailing zeros, e.g. 3.0 -> "3", 3.256 -> "3.26". */
fun formatNumber(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** Short capitalized units like "V" are grades: written in front ("V5") and never summed. */
val Activity.hasGradeUnit: Boolean
    get() = kind == ActivityKind.NUMBER && unit.length in 1..2 && unit.first().isUpperCase()

/** How a value reads for this activity, e.g. "Yes", "3.1 mi", "V5", "1:30". */
fun Activity.formatValue(value: Double): String = when (kind) {
    ActivityKind.BOOLEAN -> if (value > 0) "Yes" else "No"
    ActivityKind.DURATION -> formatDuration(value)
    ActivityKind.NUMBER -> when {
        unit.isEmpty() -> formatNumber(value)
        hasGradeUnit -> "$unit${formatNumber(value)}"
        else -> "${formatNumber(value)} $unit"
    }
}
