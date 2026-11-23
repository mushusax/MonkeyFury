package com.mushusax.monkeyfury

import android.app.Application
import com.mushusax.monkeyfury.data.GoogleAuthClient
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import com.mushusax.monkeyfury.data.repository.ActivityRepository
import com.mushusax.monkeyfury.data.repository.FlashcardRepository
import com.mushusax.monkeyfury.data.repository.NetworkActivityRepository
import com.mushusax.monkeyfury.data.repository.NetworkFlashcardRepository
import com.mushusax.monkeyfury.data.repository.NetworkUserRepository
import com.mushusax.monkeyfury.data.repository.UserRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MonkeyFuryApplication : Application() {

    val json = Json {
        ignoreUnknownKeys = true // Prevents crashes if backend adds new fields
    }

    val googleAuthClient: GoogleAuthClient by lazy { GoogleAuthClient(this) }

    val api: MonkeyFuryApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = userRepository.token
                val request = if (token == null) chain.request()
                else chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.munkyfury.app")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MonkeyFuryApi::class.java)
    }

    val userRepository: UserRepository by lazy {
        NetworkUserRepository(api, googleAuthClient)
    }

    val activityRepository: ActivityRepository by lazy {
        NetworkActivityRepository(api)
    }

    val flashcardRepository: FlashcardRepository by lazy {
        NetworkFlashcardRepository()
    }
}
