package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Media
import com.mushusax.monkeyfury.data.MediaLogEntry
import com.mushusax.monkeyfury.data.MediaParams
import com.mushusax.monkeyfury.data.ProgressParams
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.Response
import java.time.LocalDate

class NetworkMediaRepository(private val api: MonkeyFuryApi) : MediaRepository {

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    override val media: StateFlow<List<Media>> = _media.asStateFlow()

    override suspend fun refreshMedia() {
        _media.value = api.getMedia().bodyOrThrow()
    }

    override suspend fun createMedia(params: MediaParams): Media {
        val created = api.createMedia(params).bodyOrThrow()
        _media.update { listOf(created) + it }
        return created
    }

    override suspend fun updateMedia(id: String, params: MediaParams): Media =
        api.updateMedia(id, params).bodyOrThrow().also(::moveToFront)

    override suspend fun archiveMedia(id: String) {
        api.archiveMedia(id).checkSuccess()
        _media.update { list -> list.filterNot { it.publicId == id } }
    }

    override suspend fun logProgress(id: String, amount: Int, date: LocalDate): Media =
        api.logProgress(id, ProgressParams(amount, date.toString())).bodyOrThrow().also(::moveToFront)

    override suspend fun getLog(from: LocalDate, to: LocalDate): List<MediaLogEntry> =
        api.getMediaLog(from.toString(), to.toString()).bodyOrThrow()

    override fun clear() {
        _media.value = emptyList()
    }

    /** Matches the server's order, where the item just changed is the most recent. */
    private fun moveToFront(changed: Media) {
        _media.update { list -> listOf(changed) + list.filterNot { it.publicId == changed.publicId } }
    }

    private fun <T> Response<T>.bodyOrThrow(): T {
        checkSuccess()
        return body() ?: throw ActivityApiException(code(), "Empty response from server")
    }

    private fun Response<*>.checkSuccess() {
        if (!isSuccessful) {
            val detail = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: message()
            throw ActivityApiException(code(), detail.ifBlank { "Server error ${code()}" })
        }
    }
}
