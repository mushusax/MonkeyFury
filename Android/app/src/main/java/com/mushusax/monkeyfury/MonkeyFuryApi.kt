package com.mushusax.monkeyfury

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
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
        val username: String,
        val email: String,
        val name: String
    )

    @GET("/logout")
    suspend fun logout(): ResponseBody
}

