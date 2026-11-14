package com.mushusax.monkeyfury

import android.app.Application
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import com.mushusax.monkeyfury.data.repository.NetworkUserRepository
import com.mushusax.monkeyfury.data.repository.UserRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@HiltAndroidApp
class MonkeyFuryApplication : Application() {

    val json = Json {
        ignoreUnknownKeys = true // Prevents crashes if backend adds new fields
    }

    val api: MonkeyFuryApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.munkyfury.app")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MonkeyFuryApi::class.java)
    }
    val userRepository: UserRepository by lazy {
        NetworkUserRepository(api)
    }
}
