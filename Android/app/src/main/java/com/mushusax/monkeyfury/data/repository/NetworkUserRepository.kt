package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.User
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkUserRepository(val api: MonkeyFuryApi) : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(loginParams: MonkeyFuryApi.LoginParams) {
        val response = api.login(loginParams)
        if (response.isSuccessful)
            _currentUser.value = response.body()
        else _currentUser.value = null
    }

    override suspend fun logout() {
        if (_currentUser.value == null) return

        val logoutParams = MonkeyFuryApi.LogoutParams(_currentUser.value!!)
        val response = api.logout(logoutParams)

        if (response.isSuccessful) {
            _currentUser.value = null
        }
    }
}