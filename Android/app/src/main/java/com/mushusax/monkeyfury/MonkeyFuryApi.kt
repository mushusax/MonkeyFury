package com.mushusax.monkeyfury

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface MonkeyFuryApi {
    @POST("/login")
    suspend fun login(@Body requestBody: LoginParams): User

    @Serializable
    data class LoginParams(
        val username: String
    )

    @Serializable
    data class User(
        val id: Int,
        val username: String,
        val email: String,
        val name: String
    )

    @Serializable
    data class LogoutParams(
        val user: User
    )

    @POST("/logout")
    suspend fun logout(@Body param: LogoutParams): User
}

