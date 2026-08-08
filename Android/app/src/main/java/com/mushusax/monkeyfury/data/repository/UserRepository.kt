package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.User
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(loginParams: MonkeyFuryApi.LoginParams)
    suspend fun logout()
}