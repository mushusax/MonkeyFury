package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityKind
import com.mushusax.monkeyfury.ui.component.ActivityWallpaper
import com.mushusax.monkeyfury.ui.component.ContentPanel
import com.mushusax.monkeyfury.data.formatValue
import com.mushusax.monkeyfury.data.hasGradeUnit
import com.mushusax.monkeyfury.ui.component.CalendarHeatMap
import com.mushusax.monkeyfury.ui.viewmodel.ActivityCalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")

/** One heatmap per activity for each month; swipe right to go back in time. */
@Composable
fun ActivityCalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityCalendarViewModel = viewModel(factory = ActivityCalendarViewModel.Factory)
) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val values by viewModel.values.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val months = viewModel.months
    // Start on the current month
    val pagerState = rememberPagerState(initialPage = months.lastIndex) { months.size }

    ActivityWallpaper(modifier = modifier) {
        ContentPanel(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            error?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(modifier = Modifier.weight(1f), text = it, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.load() }) { Text("Retry") }
                }
            }
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = pagerState
            ) { page ->
                val month = months[page]
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = month.format(MONTH_YEAR_FORMATTER),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    if (activities.isEmpty() && !loading) {
                        item { Text("No activities yet. Add some from Activity Tracking.") }
                    }
                    items(items = activities, key = { it.publicId }) { activity ->
                        ActivityMonth(activity = activity, month = month, values = values[activity.publicId].orEmpty())
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMonth(activity: Activity, month: YearMonth, values: Map<LocalDate, Float>) {
    val monthValues = values.filterKeys { YearMonth.from(it) == month }
    val intensity = heatmapIntensity(activity, monthValues)
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = activity.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = monthSummary(activity, monthValues),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CalendarHeatMap(
            values = monthValues,
            month = month,
            intensity = intensity,
            cellSize = 28.dp,
            valueLabel = { activity.formatValue(it.toDouble()) },
            markMissingToday = true,
            markMissingPast = true
        )
    }
}

/**
 * Maps a day's value to 0 (empty) through 1 (full color). Higher is better: full at the goal, or at
 * the month's highest day without one. Lower is better: full at 0, empty at the limit, or at the
 * month's highest day without one.
 */
private fun heatmapIntensity(activity: Activity, monthValues: Map<LocalDate, Float>): (Float) -> Float {
    if (activity.kind == ActivityKind.BOOLEAN) {
        return if (activity.lowerIsBetter) { v -> if (v > 0f) 0f else 1f } else { v -> if (v > 0f) 1f else 0f }
    }
    val scale = activity.goal?.toFloat() ?: monthValues.values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    return if (activity.lowerIsBetter) { v -> 1f - v / scale } else { v -> v / scale }
}

private fun dayCount(count: Int) = if (count == 1) "1 day" else "$count days"

/**
 * e.g. "12 days" for yes/no, "12 days · 6:30 total" for durations, "3 days · best V5" for grades,
 * and "20 logged · 15 within limit · 9 drinks total" when lower is better.
 */
private fun monthSummary(activity: Activity, values: Map<LocalDate, Float>): String {
    if (activity.lowerIsBetter) return lowerIsBetterSummary(activity, values)
    val activeDays = values.values.count { it > 0f }
    val days = dayCount(activeDays)
    if (activity.kind == ActivityKind.BOOLEAN || activeDays == 0) return days
    val total = activity.formatValue(values.values.sum().toDouble())
    val best = activity.formatValue(values.values.max().toDouble())
    return when {
        activity.kind == ActivityKind.DURATION -> "$days · $total total"
        activity.hasGradeUnit -> "$days · best $best"
        else -> "$days · $total total · best $best"
    }
}

private fun lowerIsBetterSummary(activity: Activity, values: Map<LocalDate, Float>): String {
    val logged = "${values.size} logged"
    if (values.isEmpty()) return logged
    if (activity.kind == ActivityKind.BOOLEAN) return "$logged · ${values.values.count { it == 0f }} no"
    val limit = activity.goal?.toFloat()
    val good = if (limit != null) {
        "${values.values.count { it <= limit }} within limit"
    } else "${values.values.count { it == 0f }} at 0"
    val total = activity.formatValue(values.values.sum().toDouble())
    return "$logged · $good · $total total"
}
