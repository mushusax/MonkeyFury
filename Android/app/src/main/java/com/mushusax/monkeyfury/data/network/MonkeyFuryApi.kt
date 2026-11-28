package com.mushusax.monkeyfury.data.network

import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityEntry
import com.mushusax.monkeyfury.data.ActivityParams
import com.mushusax.monkeyfury.data.AnswerParams
import com.mushusax.monkeyfury.data.Card
import com.mushusax.monkeyfury.data.EntryValueParams
import com.mushusax.monkeyfury.data.FlashcardData
import com.mushusax.monkeyfury.data.Media
import com.mushusax.monkeyfury.data.MediaLogEntry
import com.mushusax.monkeyfury.data.MediaParams
import com.mushusax.monkeyfury.data.NoteParams
import com.mushusax.monkeyfury.data.NoteResult
import com.mushusax.monkeyfury.data.ProgressParams
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

    /** Ends every MonkeyFury session for the signed-in user; their JWTs stop working. */
    @POST("/auth/logout")
    suspend fun logout(): Response<Unit>

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

    /** Every deck the signed-in user owns, with all of its notes and cards. [zone] decides when today began. */
    @GET("/flashcards")
    suspend fun getFlashcards(@Query("zone") zone: String): Response<FlashcardData>

    /** Reschedules the card with FSRS on the server and logs the answer. */
    @POST("/cards/{id}/answer")
    suspend fun answerCard(@Path("id") id: String, @Body body: AnswerParams): Response<Card>

    /** Creates the note's card, plus a reverse card when it's reversible. */
    @POST("/decks/{id}/notes")
    suspend fun createNote(@Path("id") deckId: String, @Body body: NoteParams): Response<NoteResult>

    /** Keeps existing schedules; adds or deletes the reverse card when reversible changes. */
    @PUT("/notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body body: NoteParams): Response<NoteResult>

    @DELETE("/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Unit>

    /** The library, most recently touched first. */
    @GET("/media")
    suspend fun getMedia(): Response<List<Media>>

    @POST("/media")
    suspend fun createMedia(@Body body: MediaParams): Response<Media>

    @PUT("/media/{id}")
    suspend fun updateMedia(@Path("id") id: String, @Body body: MediaParams): Response<Media>

    @DELETE("/media/{id}")
    suspend fun archiveMedia(@Path("id") id: String): Response<Unit>

    /** Adds to progress and that day's log row; the server moves the status along. */
    @POST("/media/{id}/progress")
    suspend fun logProgress(@Path("id") id: String, @Body body: ProgressParams): Response<Media>

    /** [from] and [to] are yyyy-MM-dd, both inclusive. */
    @GET("/media/log")
    suspend fun getMediaLog(@Query("from") from: String, @Query("to") to: String): Response<List<MediaLogEntry>>
}
