package com.mushusax.monkeyfury.data.network

import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityEntry
import com.mushusax.monkeyfury.data.ActivityParams
import com.mushusax.monkeyfury.data.EntryValueParams
import com.mushusax.monkeyfury.data.User
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MonkeyFuryApi {

    @Serializable
    data class GoogleLoginParams(
        val idToken: String
    )

    @Serializable
    data class LoginResponse(
        val token: String,
        val user: User
    )

    @POST("/auth/google")
    suspend fun loginWithGoogle(@Body body: GoogleLoginParams): Response<LoginResponse>

    @GET("/activities")
    suspend fun getActivities(): Response<List<Activity>>

    @POST("/activities")
    suspend fun createActivity(@Body body: ActivityParams): Response<Activity>

    @PUT("/activities/{id}")
    suspend fun updateActivity(@Path("id") id: String, @Body body: ActivityParams): Response<Activity>

    @DELETE("/activities/{id}")
    suspend fun archiveActivity(@Path("id") id: String): Response<Unit>

    /** [from] and [to] are yyyy-MM-dd, both inclusive. */
    @GET("/activities/entries")
    suspend fun getEntries(@Query("from") from: String, @Query("to") to: String): Response<List<ActivityEntry>>

    @PUT("/activities/{id}/entries/{date}")
    suspend fun saveEntry(
        @Path("id") id: String,
        @Path("date") date: String,
        @Body body: EntryValueParams
    ): Response<ActivityEntry>

    @DELETE("/activities/{id}/entries/{date}")
    suspend fun deleteEntry(@Path("id") id: String, @Path("date") date: String): Response<Unit>
}
