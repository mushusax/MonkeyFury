package com.mushusax.monkeyfury.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mushusax.monkeyfury.MonkeyFuryApi
import com.mushusax.monkeyfury.MonkeyFuryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    var uiState: MutableStateFlow<LogoutUiState> = MutableStateFlow(LogoutUiState.Error(null))
    fun logOut(user: MonkeyFuryApi.User) {
        viewModelScope.launch {
            runCatching {
                (application as MonkeyFuryApplication).api.logout(MonkeyFuryApi.LogoutParams(user))
            }.onSuccess { user ->
                if (!user.username.equals("User not found")) {
                    uiState.value = LogoutUiState.Success(user)
                }
            }.onFailure { exception ->
                uiState.value = LogoutUiState.Error(exception)
            }
        }
    }
}

sealed class LogoutUiState {
    data class Success(val user: MonkeyFuryApi.User) : LogoutUiState()
    data class Error(val exception: Throwable?) : LogoutUiState()
}