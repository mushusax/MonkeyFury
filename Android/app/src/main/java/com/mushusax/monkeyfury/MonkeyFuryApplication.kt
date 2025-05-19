package com.mushusax.monkeyfury

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import retrofit2.Retrofit

@HiltAndroidApp
class MonkeyFuryApplication : Application() {

    lateinit var weatherService: WeatherApi

    override fun onCreate() {
        super.onCreate()

        weatherService = Retrofit.Builder()
            .baseUrl("http://192.168.1.6:3000")
            .build()
            .create(WeatherApi::class.java)
    }
}
