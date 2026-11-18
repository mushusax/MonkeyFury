package com.mushusax.monkeyfury.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushusax.monkeyfury.ui.component.CalendarHeatMap
import com.mushusax.monkeyfury.ui.component.Spinner
import com.mushusax.monkeyfury.ui.viewmodel.GraphScreenViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private const val MONTH_COUNT = 12
private val SAMPLE_HABITS = listOf("Habit 1", "Habit 2", "Habit 3")
private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun GraphScreen(
    modifier: Modifier = Modifier,
    viewModel: GraphScreenViewModel = viewModel()
) {
    var selectedHabit by remember { mutableStateOf(SAMPLE_HABITS.first()) }
    // The last 12 months, oldest first, ending with the current month
    val months = remember {
        val current = YearMonth.now()
        (MONTH_COUNT - 1 downTo 0).map { current.minusMonths(it.toLong()) }
    }
    // Sample data: a random value 0 through 5 for each day of every month
    val sampleValues = remember(months) {
        val values = mutableMapOf<LocalDate, Int>()
        months.forEach { month ->
            (1..month.lengthOfMonth()).forEach { day ->
                values[month.atDay(day)] = Random.nextInt(0, 6)
            }
        }
        values
    }
    // Start on the current month; swipe right to go back in time
    val pagerState = rememberPagerState(initialPage = months.lastIndex) { months.size }

    Column(modifier = modifier) {
        Spinner(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            options = SAMPLE_HABITS,
            selectedOption = selectedHabit,
            onOptionSelected = { selectedHabit = it },
            label = "Habit"
        )
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = pagerState
        ) { page ->
            val month = months[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = month.format(MONTH_YEAR_FORMATTER),
                    style = MaterialTheme.typography.titleLarge
                )
                CalendarHeatMap(
                    values = sampleValues,
                    month = month
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewGraphScreen() {
    GraphScreen(modifier = Modifier.fillMaxSize())
}
