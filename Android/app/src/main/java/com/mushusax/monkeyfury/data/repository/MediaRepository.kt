package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Media
import com.mushusax.monkeyfury.data.MediaLogEntry
import com.mushusax.monkeyfury.data.MediaParams
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Books, anime and movies, and the daily log behind their heat map. Every call throws
 * [java.io.IOException] when the server can't be reached and [ActivityApiException] when it
 * answers with an error.
 */
interface MediaRepository {
    /** The latest library, most recently touched first, shared by every screen. */
    val media: StateFlow<List<Media>>

    suspend fun refreshMedia()
    suspend fun createMedia(params: MediaParams): Media

    /** Sets progress directly without writing to the log, for fixing a count. */
    suspend fun updateMedia(id: String, params: MediaParams): Media
    suspend fun archiveMedia(id: String)

    /**
     * Logs [amount] pages or episodes on [date]; negative undoes. The server starts, completes or
     * reopens the item as needed, so use the returned item. Not for movies.
     */
    suspend fun logProgress(id: String, amount: Int, date: LocalDate = LocalDate.now()): Media

    /** Log entries from [from] through [to], inclusive; at most 400 days. */
    suspend fun getLog(from: LocalDate, to: LocalDate): List<MediaLogEntry>

    /** Forgets cached media, e.g. on logout, so the next user never sees it. */
    fun clear()
}
