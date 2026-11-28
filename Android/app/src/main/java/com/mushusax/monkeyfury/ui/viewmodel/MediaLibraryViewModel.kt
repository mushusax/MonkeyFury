package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.Media
import com.mushusax.monkeyfury.data.MediaKind
import com.mushusax.monkeyfury.data.MediaParams
import com.mushusax.monkeyfury.data.MediaStatus
import com.mushusax.monkeyfury.data.repository.ActivityApiException
import com.mushusax.monkeyfury.data.repository.MediaRepository
import com.mushusax.monkeyfury.data.repository.toUserMessage
import com.mushusax.monkeyfury.data.toParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate

class MediaLibraryViewModel(private val mediaRepository: MediaRepository) : ViewModel() {

    /** The kind tab that's selected; null is All. */
    private val _kindFilter = MutableStateFlow<MediaKind?>(null)
    val kindFilter: StateFlow<MediaKind?> = _kindFilter.asStateFlow()

    /** The filtered library in sections, in [MediaStatus] order, empty sections left out. */
    val sections: StateFlow<List<Pair<MediaStatus, List<Media>>>> =
        combine(mediaRepository.media, _kindFilter) { media, kind ->
            groupByStatus(if (kind == null) media else media.filter { it.kind == kind })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = run { mediaRepository.refreshMedia() }

    fun selectKind(kind: MediaKind?) {
        _kindFilter.value = kind
    }

    /** Creates when [id] is null, otherwise updates. Calls [onDone] only on success. */
    fun save(id: String?, params: MediaParams, onDone: () -> Unit) = run {
        if (id == null) mediaRepository.createMedia(params) else mediaRepository.updateMedia(id, params)
        onDone()
    }

    fun archive(id: String, onDone: () -> Unit) = run {
        mediaRepository.archiveMedia(id)
        onDone()
    }

    /** Logs pages or episodes today; negative undoes. */
    fun logProgress(id: String, amount: Int, onDone: () -> Unit = {}) = run {
        mediaRepository.logProgress(id, amount)
        onDone()
    }

    /** Completes a movie today, thumbs up unless it was already rated. */
    fun markWatched(movie: Media) = run {
        val params = movie.toParams()
        mediaRepository.updateMedia(
            movie.publicId,
            params.copy(
                status = MediaStatus.COMPLETED,
                finishedOn = LocalDate.now().toString(),
                thumbsUp = params.thumbsUp ?: true
            )
        )
    }

    fun clearError() {
        _error.value = null
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                block()
            } catch (e: IOException) {
                _error.value = e.toUserMessage()
            } catch (e: ActivityApiException) {
                _error.value = e.toUserMessage()
            } finally {
                _loading.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                MediaLibraryViewModel(mediaRepository = app.mediaRepository)
            }
        }
    }
}

/** Sections in [MediaStatus] order; within one, the most recently touched first, as the server sends them. */
internal fun groupByStatus(media: List<Media>): List<Pair<MediaStatus, List<Media>>> {
    val byStatus = media.groupBy { it.status }
    return MediaStatus.entries.mapNotNull { status -> byStatus[status]?.let { status to it } }
}
