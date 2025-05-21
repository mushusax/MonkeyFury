package com.mushusax.monkeyfury

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET


interface MonkeyFuryApi {
    @GET("/login")
    fun login(): Call<ResponseBody>

    @GET("/logout")
    fun logout(): Call<ResponseBody>
}