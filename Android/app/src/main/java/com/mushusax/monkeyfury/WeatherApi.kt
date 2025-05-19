package com.mushusax.monkeyfury

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET


interface WeatherApi {
    @GET("/weatherforecast")
    fun getWeather(): Call<ResponseBody>
}