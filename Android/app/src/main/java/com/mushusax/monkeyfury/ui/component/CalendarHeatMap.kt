package com.mushusax.monkeyfury.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
    values: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    month: YearMonth = YearMonth.now(),
    maxValue: Int = 5,
    cellSize: Dp = 32.dp,
    cellSpacing: Dp = 4.dp
) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val fullColor = MaterialTheme.colorScheme.primary
    val daysInMonth = month.lengthOfMonth()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
        for (row in 0 until ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
                for (column in 0 until COLUMNS) {
                    val dayIndex = row * COLUMNS + column
                    if (dayIndex < daysInMonth) {
                        val date = month.atDay(dayIndex + 1)
                        val value = values[date] ?: 0
                        val fraction = if (maxValue > 0) {
                            (value.toFloat() / maxValue).coerceIn(0f, 1f)
                        } else 0f
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(4.dp))
                                .background(lerp(emptyColor, fullColor, fraction))
                                .semantics {
                                    contentDescription = "${date.format(DAY_FORMATTER)}: $value"
                                }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarHeatMapPreview() {
    val month = YearMonth.now()
    val random = Random(seed = 42)
    val sampleValues = (1..month.lengthOfMonth()).associate { day ->
        month.atDay(day) to random.nextInt(0, 6)
    }
    CalendarHeatMap(
        modifier = Modifier.padding(16.dp),
        values = sampleValues,
        month = month
    )
}
