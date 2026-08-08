package com.mushusax.monkeyfury.data.network

import com.mushusax.monkeyfury.data.User
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MonkeyFuryApi {

    @Serializable
    data class LoginParams(
        val username: String
    )

    @POST("/login")
    suspend fun login(@Body requestBody: LoginParams): Response<User?>

    @Serializable
    data class LogoutParams(
        val user: User
    )

    @POST("/logout")
    suspend fun logout(@Body param: LogoutParams): Response<Unit>
}