package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.Media
import com.mushusax.monkeyfury.data.MediaKind
import com.mushusax.monkeyfury.data.MediaLogEntry
import com.mushusax.monkeyfury.data.MediaStatus
import com.mushusax.monkeyfury.data.repository.ActivityApiException
import com.mushusax.monkeyfury.data.repository.MediaRepository
import com.mushusax.monkeyfury.data.repository.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

private const val MONTH_COUNT = 12

/** Items finished this year for one kind, and how many of those got a thumbs up. */
data class YearTotal(val finished: Int, val thumbsUp: Int)

class MediaStatsViewModel(private val mediaRepository: MediaRepository) : ViewModel() {

    /** The last 12 months, oldest first, ending with the current month. */
    val months: List<YearMonth> = YearMonth.now().let { current ->
        (MONTH_COUNT - 1 downTo 0).map { current.minusMonths(it.toLong()) }
    }

    val year: Int = LocalDate.now().year

    /** Pages, episodes or movies per day, by kind. */
    private val _values = MutableStateFlow<Map<MediaKind, Map<LocalDate, Float>>>(emptyMap())
    val values: StateFlow<Map<MediaKind, Map<LocalDate, Float>>> = _values.asStateFlow()

    val yearTotals: StateFlow<Map<MediaKind, YearTotal>> = mediaRepository.media
        .map { yearTotals(it, year) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, yearTotals(mediaRepository.media.value, year))

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    /** Loads all 12 months in one request, so swiping between months needs no network. */
    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // The log names items by id, so the library is needed to tell books from anime
                mediaRepository.refreshMedia()
                val log = mediaRepository.getLog(months.first().atDay(1), months.last().atEndOfMonth())
                _values.value = valuesByKind(log, mediaRepository.media.value)
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
                MediaStatsViewModel(mediaRepository = app.mediaRepository)
            }
        }
    }
}

/** Sums each day's log across items of the same kind; entries for unknown items are skipped. */
internal fun valuesByKind(log: List<MediaLogEntry>, media: List<Media>): Map<MediaKind, Map<LocalDate, Float>> {
    val kindById = media.associate { it.publicId to it.kind }
    return log.mapNotNull { entry -> kindById[entry.mediaId]?.let { it to entry } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, entries) ->
            entries.groupBy { LocalDate.parse(it.date) }.mapValues { (_, day) -> day.sumOf { it.amount }.toFloat() }
        }
}

internal fun yearTotals(media: List<Media>, year: Int): Map<MediaKind, YearTotal> {
    val finished = media.filter { item ->
        item.status == MediaStatus.COMPLETED && item.finishedOn?.let { LocalDate.parse(it).year == year } == true
    }
    return MediaKind.entries.associateWith { kind ->
        val ofKind = finished.filter { it.kind == kind }
        YearTotal(finished = ofKind.size, thumbsUp = ofKind.count { it.thumbsUp == true })
    }
}
