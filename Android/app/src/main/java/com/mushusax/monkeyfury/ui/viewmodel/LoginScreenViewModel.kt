package com.mushusax.monkeyfury.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mushusax.monkeyfury.LoginRequest
import com.mushusax.monkeyfury.LoginResponse
import com.mushusax.monkeyfury.MonkeyFuryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginScreenViewModel(application: Application) : AndroidViewModel(application) {

    var uiState: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState.Error(null))

    fun logIn(username: String) {
        viewModelScope.launch {
            runCatching {
                (application as MonkeyFuryApplication).api.login(
                    LoginRequest(username.trim())
                )
            }.onSuccess { responseBody ->
                if (!responseBody.message.equals("User not found")) {
                    uiState.value = LoginUiState.Success(responseBody)
                }
            }.onFailure { exception ->
                uiState.value = LoginUiState.Error(exception)
            }
        }
    }
}

sealed class LoginUiState {
    data class Success(val responseBody: LoginResponse) : LoginUiState()
    data class Error(val exception: Throwable?) : LoginUiState()
}