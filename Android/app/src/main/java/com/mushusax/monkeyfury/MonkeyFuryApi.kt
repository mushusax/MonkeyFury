package com.mushusax.monkeyfury

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface MonkeyFuryApi {
    @GET("/")
    fun healthCheck(): Call<ResponseBody>

    @POST("/login")
    fun login(@Body requestBody: RequestBody): Call<ResponseBody>

    @GET("/logout")
    fun logout(): Call<ResponseBody>
}