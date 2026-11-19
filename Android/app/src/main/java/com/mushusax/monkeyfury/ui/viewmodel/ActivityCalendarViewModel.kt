package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.repository.ActivityApiException
import com.mushusax.monkeyfury.data.repository.ActivityRepository
import com.mushusax.monkeyfury.data.repository.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

private const val MONTH_COUNT = 12

class ActivityCalendarViewModel(private val activityRepository: ActivityRepository) : ViewModel() {

    val activities: StateFlow<List<Activity>> = activityRepository.activities

    /** The last 12 months, oldest first, ending with the current month. */
    val months: List<YearMonth> = YearMonth.now().let { current ->
        (MONTH_COUNT - 1 downTo 0).map { current.minusMonths(it.toLong()) }
    }

    /** Entry values by activity id, then by date. */
    private val _values = MutableStateFlow<Map<String, Map<LocalDate, Float>>>(emptyMap())
    val values: StateFlow<Map<String, Map<LocalDate, Float>>> = _values.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Loads all 12 months in one request, so swiping between months needs no network. */
    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                if (activityRepository.activities.value.isEmpty()) activityRepository.refreshActivities()
                val entries = activityRepository.getEntries(months.first().atDay(1), months.last().atEndOfMonth())
                _values.value = entries.groupBy { it.activityId }.mapValues { (_, list) ->
                    list.associate { LocalDate.parse(it.date) to it.value.toFloat() }
                }
            } catch (e: IOException) {
                _error.value = e.toUserMessage()
            } catch (e: ActivityApiException) {
                _error.value = e.toUserMessage()
            } finally {
                _loading.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                ActivityCalendarViewModel(activityRepository = app.activityRepository)
            }
        }
    }
}
