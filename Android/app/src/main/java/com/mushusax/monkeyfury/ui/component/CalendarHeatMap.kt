package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private const val ROWS = 5
private const val COLUMNS = 7
private val DAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun CalendarHeatMap(
    values: Map<LocalDate, Float>,
    modifier: Modifier = Modifier,
    month: YearMonth = YearMonth.now(),
    // How strongly a recorded day is colored, from 0 (empty) to 1 (full); days with no value stay empty
    intensity: (Float) -> Float = { it / 5f },
    cellSize: Dp = 32.dp,
    cellSpacing: Dp = 4.dp,
    // How a day's value is read out by screen readers; days with no value read "no entry"
    valueLabel: (Float) -> String = { it.toString() },
    // Today with no value is drawn in the tertiary color, as a reminder to fill it in
    markMissingToday: Boolean = false,
    // Past days with no value show "?" instead of looking like an empty (worst) day
    markMissingPast: Boolean = false,
    // The short text shown in a tapped day; whole values drop the ".0"
    cellLabel: (Float) -> String = ::formatCellValue,
    today: LocalDate = LocalDate.now()
) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val fullColor = MaterialTheme.colorScheme.primary
    val pendingColor = MaterialTheme.colorScheme.tertiary
    val daysInMonth = month.lengthOfMonth()
    // The tapped day, whose value shows in its square; one at a time, and cleared on a new month
    var shownDate by remember(month) { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
        for (row in 0 until ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
                for (column in 0 until COLUMNS) {
                    val dayIndex = row * COLUMNS + column
                    if (dayIndex < daysInMonth) {
                        val date = month.atDay(dayIndex + 1)
                        val value = values[date]
                        val fraction = value?.let(intensity)?.coerceIn(0f, 1f) ?: 0f
                        val pending = value == null && markMissingToday && date == today
                        val unknown = value == null && markMissingPast && date.isBefore(today)
                        val shown = value != null && date == shownDate
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (pending) pendingColor else lerp(emptyColor, fullColor, fraction))
                                // Tapping shows the day's value; tapping it again, or an empty day, hides it
                                .clickable(onClickLabel = "Show value") {
                                    shownDate = if (shown || value == null) null else date
                                }
                                .semantics {
                                    contentDescription = "${date.format(DAY_FORMATTER)}: " + when {
                                        value != null -> valueLabel(value)
                                        pending -> "not filled in yet"
                                        else -> "no entry"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (shown) {
                                Text(
                                    text = cellLabel(value),
                                    style = MaterialTheme.typography.labelSmall,
                                    // Light text on the fuller, darker squares; dark on the paler ones
                                    color = if (fraction > 0.5f) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            } else if (unknown) {
                                Text(
                                    text = "?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}

/** 3 for 3.0, 2.5 for 2.5. */
private fun formatCellValue(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)

@Preview(showBackground = true)
@Composable
fun CalendarHeatMapPreview() {
    val month = YearMonth.now()
    val random = Random(seed = 42)
    val sampleValues = (1..month.lengthOfMonth()).associate { day ->
        month.atDay(day) to random.nextInt(0, 6).toFloat()
    }
    CalendarHeatMap(
        modifier = Modifier.padding(16.dp),
        values = sampleValues,
        month = month
    )
}
