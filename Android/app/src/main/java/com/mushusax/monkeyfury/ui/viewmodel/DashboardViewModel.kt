package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.User
import com.mushusax.monkeyfury.data.repository.ActivityRepository
import com.mushusax.monkeyfury.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.currentUser
    fun logOut() {
        viewModelScope.launch {
            userRepository.logout()
            activityRepository.clear()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                DashboardViewModel(
                    userRepository = app.userRepository,
                    activityRepository = app.activityRepository
                )
            }
        }
    }
}