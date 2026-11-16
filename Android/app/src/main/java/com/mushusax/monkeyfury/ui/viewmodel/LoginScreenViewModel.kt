package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.GoogleAuthClient
import com.mushusax.monkeyfury.data.User
import com.mushusax.monkeyfury.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class LoginScreenViewModel(
    private val userRepository: UserRepository,
    val googleAuthClient: GoogleAuthClient,
) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.currentUser

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun onGoogleIdToken(idToken: String) {
        _error.value = null
        viewModelScope.launch {
            try {
                if (!userRepository.loginWithGoogle(idToken)) _error.value = "Sign-in was rejected"
            } catch (_: IOException) {
                _error.value = "Couldn't reach the server"
            }
        }
    }

    fun onSignInFailed(message: String) {
        _error.value = message
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                LoginScreenViewModel(app.userRepository, app.googleAuthClient)
            }
        }
    }
}
