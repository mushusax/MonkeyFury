package com.mushusax.monkeyfury.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.mushusax.monkeyfury.MonkeyFuryApplication
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    fun logOut() {
        viewModelScope.launch {
            runCatching {
                (application as MonkeyFuryApplication).api.logout()
            }.onSuccess {

            }.onFailure {

            }
        }
    }

}