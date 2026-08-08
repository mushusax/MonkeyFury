package com.mushusax.monkeyfury

import android.app.Application
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import dagger.hilt.android.HiltAndroidApp
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@HiltAndroidApp
class MonkeyFuryApplication : Application() {

    lateinit var api: MonkeyFuryApi

    override fun onCreate() {
        super.onCreate()
        val json = Json {
            ignoreUnknownKeys = true // Prevents crashes if backend adds new fields
        }

        api = Retrofit.Builder()
            .baseUrl("http://0.0.0.0:8080")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MonkeyFuryApi::class.java)
    }
}
