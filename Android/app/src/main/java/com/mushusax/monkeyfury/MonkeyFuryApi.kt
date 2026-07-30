package com.mushusax.monkeyfury

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface MonkeyFuryApi {
    @POST("/login")
    suspend fun login(@Body requestBody: LoginRequest): LoginResponse

    @GET("/logout")
    suspend fun logout(): ResponseBody
}

@Serializable
data class LoginRequest(
    val username: String
)

@Serializable
data class LoginResponse(
    val message: String? = null
)