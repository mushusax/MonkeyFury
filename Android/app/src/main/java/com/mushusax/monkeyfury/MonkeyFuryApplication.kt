package com.mushusax.monkeyfury

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import retrofit2.Retrofit

@HiltAndroidApp
class MonkeyFuryApplication : Application() {

    lateinit var api: MonkeyFuryApi

    override fun onCreate() {
        super.onCreate()

        api = Retrofit.Builder()
            .baseUrl("http://192.168.1.13:3000")
            .build()
            .create(MonkeyFuryApi::class.java)
    }
}
