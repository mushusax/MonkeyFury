package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<User?>

    /** The MonkeyFury JWT, or null when logged out. */
    val token: String?

    /** Exchanges a Google ID token for a MonkeyFury session. Returns false if the server rejects it. */
    suspend fun loginWithGoogle(idToken: String): Boolean
    suspend fun logout()
}
