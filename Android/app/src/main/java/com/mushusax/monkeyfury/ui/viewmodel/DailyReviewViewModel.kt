package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityKind
import com.mushusax.monkeyfury.data.formatDuration
import com.mushusax.monkeyfury.data.formatNumber
import com.mushusax.monkeyfury.data.parseDuration
import com.mushusax.monkeyfury.data.repository.ActivityApiException
import com.mushusax.monkeyfury.data.repository.ActivityRepository
import com.mushusax.monkeyfury.data.repository.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

/**
 * Inputs are kept as text, keyed by activity id: "1"/"0" for BOOLEAN, h:mm for DURATION and a
 * plain number for NUMBER. An empty string means nothing is recorded for that day.
 */
class DailyReviewViewModel(private val activityRepository: ActivityRepository) : ViewModel() {

    val activities: StateFlow<List<Activity>> = activityRepository.activities

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    /** What the server has for [date]. */
    private val _saved = MutableStateFlow<Map<String, String>>(emptyMap())
    val saved: StateFlow<Map<String, String>> = _saved.asStateFlow()

    /** What's on screen, including unsaved edits. */
    private val _inputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val inputs: StateFlow<Map<String, String>> = _inputs.asStateFlow()

    /** Per-activity input errors found by the last save attempt. */
    private val _rowErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val rowErrors: StateFlow<Map<String, String>> = _rowErrors.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    private fun isDirty(activityId: String): Boolean = input(activityId) != saved(activityId)

    fun setDate(date: LocalDate) {
        if (date.isAfter(LocalDate.now()) || date == _date.value) return
        _date.value = date
        load()
    }

    fun setInput(activityId: String, text: String) {
        _inputs.update { it + (activityId to text) }
        _rowErrors.update { it - activityId }
        _message.value = null
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val date = _date.value
            withProgress {
                if (activityRepository.activities.value.isEmpty()) activityRepository.refreshActivities()
                val byId = activityRepository.activities.value.associateBy { it.publicId }
                val values = activityRepository.getEntries(date, date).mapNotNull { entry ->
                    byId[entry.activityId]?.let { entry.activityId to it.toInput(entry.value) }
                }.toMap()
                _saved.value = values
                _inputs.value = values
                _rowErrors.value = emptyMap()
                _message.value = null
            }
        }
    }

    /** Sends only the rows that changed. Stops before sending anything if a row doesn't parse. */
    fun save() {
        val date = _date.value
        val changed = activities.value.filter { isDirty(it.publicId) }
        if (changed.isEmpty()) return

        val errors = mutableMapOf<String, String>()
        val values = changed.associate { activity ->
            val text = input(activity.publicId).trim()
            val value = if (text.isEmpty()) null else activity.parseInput(text)
            if (text.isNotEmpty() && value == null) {
                errors[activity.publicId] = if (activity.kind == ActivityKind.DURATION) "Use h:mm" else "Not a number"
            }
            activity.publicId to value
        }
        if (errors.isNotEmpty()) {
            _rowErrors.value = errors
            return
        }

        viewModelScope.launch {
            withProgress {
                changed.forEach { activity ->
                    val id = activity.publicId
                    val value = values[id]
                    if (value == null) {
                        activityRepository.deleteEntry(id, date)
                        _saved.update { it - id }
                    } else {
                        val entry = activityRepository.saveEntry(id, date, value)
                        // Show what the server stored, e.g. a duration rounded to whole minutes
                        val stored = activity.toInput(entry.value)
                        _saved.update { it + (id to stored) }
                        _inputs.update { it + (id to stored) }
                    }
                }
                _message.value = "Saved"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun input(id: String) = _inputs.value[id] ?: ""
    private fun saved(id: String) = _saved.value[id] ?: ""

    private suspend fun withProgress(block: suspend () -> Unit) {
        _loading.value = true
        _error.value = null
        try {
            block()
        } catch (e: IOException) {
            _error.value = e.toUserMessage()
        } catch (e: ActivityApiException) {
            _error.value = e.toUserMessage()
        } finally {
            _loading.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                DailyReviewViewModel(activityRepository = app.activityRepository)
            }
        }
    }
}

private fun Activity.toInput(value: Double): String = when (kind) {
    ActivityKind.BOOLEAN -> if (value > 0) "1" else "0"
    ActivityKind.DURATION -> formatDuration(value)
    ActivityKind.NUMBER -> formatNumber(value)
}

private fun Activity.parseInput(text: String): Double? = when (kind) {
    ActivityKind.BOOLEAN -> if (text == "1") 1.0 else 0.0
    ActivityKind.DURATION -> parseDuration(text)
    ActivityKind.NUMBER -> text.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
}
