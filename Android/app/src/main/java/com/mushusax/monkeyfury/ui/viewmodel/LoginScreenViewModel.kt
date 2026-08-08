package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.User
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import com.mushusax.monkeyfury.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginScreenViewModel(val userRepository: UserRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.currentUser

    fun logIn(username: String) {
        viewModelScope.launch {
            userRepository.login(MonkeyFuryApi.LoginParams(username.trim()))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                LoginScreenViewModel(userRepository = app.userRepository)
            }
        }
    }
}