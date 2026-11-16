package com.mushusax.monkeyfury.data.network

import com.mushusax.monkeyfury.data.User
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MonkeyFuryApi {

    @Serializable
    data class GoogleLoginParams(
        val idToken: String
    )

    @Serializable
    data class LoginResponse(
        val token: String,
        val user: User
    )

    @POST("/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleLoginParams): Response<LoginResponse>
}
