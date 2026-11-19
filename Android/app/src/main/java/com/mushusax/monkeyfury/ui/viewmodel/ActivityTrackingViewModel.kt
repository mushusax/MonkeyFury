package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityParams
import com.mushusax.monkeyfury.data.repository.ActivityApiException
import com.mushusax.monkeyfury.data.repository.ActivityRepository
import com.mushusax.monkeyfury.data.repository.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ActivityTrackingViewModel(private val activityRepository: ActivityRepository) : ViewModel() {

    val activities: StateFlow<List<Activity>> = activityRepository.activities

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = run { activityRepository.refreshActivities() }

    /** Creates when [id] is null, otherwise updates. Calls [onDone] only on success. */
    fun save(id: String?, params: ActivityParams, onDone: () -> Unit) = run {
        if (id == null) activityRepository.createActivity(params) else activityRepository.updateActivity(id, params)
        onDone()
    }

    fun archive(id: String, onDone: () -> Unit) = run {
        activityRepository.archiveActivity(id)
        onDone()
    }

    fun clearError() {
        _error.value = null
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
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
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                ActivityTrackingViewModel(activityRepository = app.activityRepository)
            }
        }
    }
}
