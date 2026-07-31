package com.mushusax.monkeyfury.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mushusax.monkeyfury.MonkeyFuryApi
import com.mushusax.monkeyfury.MonkeyFuryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginScreenViewModel(application: Application) : AndroidViewModel(application) {

    var uiState: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState.Error(null))

    fun logIn(username: String) {
        viewModelScope.launch {
            runCatching {
                (application as MonkeyFuryApplication).api.login(
                    MonkeyFuryApi.LoginParams(username.trim())
                )
            }.onSuccess { user ->
                if (!user.username.equals("User not found")) {
                    uiState.value = LoginUiState.Success(user)
                }
            }.onFailure { exception ->
                uiState.value = LoginUiState.Error(exception)
            }
        }
    }
}

sealed class LoginUiState {
    data class Success(val user: MonkeyFuryApi.User) : LoginUiState()
    data class Error(val exception: Throwable?) : LoginUiState()
}