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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.data.MediaKind
import com.mushusax.monkeyfury.ui.component.ActivityWallpaper
import com.mushusax.monkeyfury.ui.component.CalendarHeatMap
import com.mushusax.monkeyfury.ui.component.ContentPanel
import com.mushusax.monkeyfury.ui.viewmodel.MediaStatsViewModel
import com.mushusax.monkeyfury.ui.viewmodel.YearTotal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")

/** The faintest a day with any reading or watching is drawn, so small days still show. */
private const val MIN_INTENSITY = 0.3f

/** This year's finished counts, then one heatmap per kind for each month; swipe right to go back in time. */
@Composable
fun MediaStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaStatsViewModel = viewModel(factory = MediaStatsViewModel.Factory)
) {
    val values by viewModel.values.collectAsStateWithLifecycle()
    val yearTotals by viewModel.yearTotals.collectAsStateWithLifecycle()
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

            YearSummary(year = viewModel.year, totals = yearTotals)

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
                    items(items = MediaKind.entries, key = { it.name }) { kind ->
                        KindMonth(kind = kind, month = month, values = values[kind].orEmpty())
                    }
                }
            }
        }
    }
}

@Composable
private fun YearSummary(year: Int, totals: Map<MediaKind, YearTotal>) {
    val finished = MediaKind.entries.map { kind ->
        val count = totals[kind]?.finished ?: 0
        "$count ${if (count == 1) kind.label.lowercase() else kind.pluralLabel.lowercase()}"
    }
    val total = totals.values.sumOf { it.finished }
    val liked = totals.values.sumOf { it.thumbsUp }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Finished in $year",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = finished.joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (total > 0) {
            Text(
                text = "👍 $liked of $total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KindMonth(kind: MediaKind, month: YearMonth, values: Map<LocalDate, Float>) {
    val monthValues = values.filterKeys { YearMonth.from(it) == month }
    // Scaled to the month's biggest day, so a 200-page day doesn't wash out the 10-page ones
    val busiest = monthValues.values.maxOrNull() ?: 1f
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = kind.pluralLabel, style = MaterialTheme.typography.titleMedium)
        Text(
            text = countLabel(kind, monthValues.values.sum().toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CalendarHeatMap(
            values = monthValues,
            month = month,
            intensity = { MIN_INTENSITY + (1f - MIN_INTENSITY) * (it / busiest) },
            cellSize = 28.dp,
            valueLabel = { countLabel(kind, it.toInt()) },
            markMissingToday = true,
            markMissingPast = true
        )
    }
}

/** "1 page", "12 episodes", "3 movies". */
private fun countLabel(kind: MediaKind, count: Int): String {
    val unit = when (kind) {
        MediaKind.BOOK -> "page"
        MediaKind.ANIME -> "episode"
        MediaKind.MOVIE -> "movie"
    }
    return "$count $unit${if (count == 1) "" else "s"}"
}
