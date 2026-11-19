package com.mushusax.monkeyfury.data.repository

import androidx.credentials.exceptions.ClearCredentialException
import com.mushusax.monkeyfury.data.GoogleAuthClient
import com.mushusax.monkeyfury.data.User
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class NetworkUserRepository(
    private val api: MonkeyFuryApi,
    private val googleAuthClient: GoogleAuthClient,
) : UserRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    @Volatile
    override var token: String? = null
        private set

    override suspend fun loginWithGoogle(idToken: String): Boolean {
        val response = api.loginWithGoogle(MonkeyFuryApi.GoogleLoginParams(idToken))
        val body = response.body()
        if (!response.isSuccessful || body == null) return false
        token = body.token
        _currentUser.value = body.user
        return true
    }

    override suspend fun logout() {
        // Tell the server first, while the token is still attached to requests
        if (token != null) {
            try {
                api.logout()
            } catch (_: IOException) {
                // Offline: still sign out locally. The token is dropped below and expires on its own.
            }
        }
        token = null
        _currentUser.value = null
        try {
            // Forgets the chosen Google account, so the next sign-in shows the account picker
            googleAuthClient.clearCredentialState()
        } catch (_: ClearCredentialException) {
            // Nothing is stored to clear; already signed out locally
        }
    }
}
