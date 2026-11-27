package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Card
import com.mushusax.monkeyfury.data.CardState
import com.mushusax.monkeyfury.data.Deck
import com.mushusax.monkeyfury.data.DueCounts
import com.mushusax.monkeyfury.data.Note
import com.mushusax.monkeyfury.data.NoteParams
import com.mushusax.monkeyfury.data.NoteResult
import com.mushusax.monkeyfury.data.Rating
import com.mushusax.monkeyfury.data.StudyCard
import com.mushusax.monkeyfury.data.StudyConfig
import com.mushusax.monkeyfury.data.AnswerParams
import com.mushusax.monkeyfury.data.network.MonkeyFuryApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * [refreshDecks] loads everything from GET /flashcards; note edits and answers are saved to the
 * server, which runs FSRS. Decks are created and changed on the server directly, not from the app.
 */
class NetworkFlashcardRepository(
    private val api: MonkeyFuryApi,
    private val zone: ZoneId = ZoneId.systemDefault()
) : FlashcardRepository {

    private val mutex = Mutex()

    private val _decks = MutableStateFlow<List<Deck>>(emptyList())
    override val decks: StateFlow<List<Deck>> = _decks.asStateFlow()

    private val _reviewDays = MutableStateFlow<Map<String, Map<LocalDate, Int>>>(emptyMap())
    override val reviewDays: StateFlow<Map<String, Map<LocalDate, Int>>> = _reviewDays.asStateFlow()

    private val _dueCounts = MutableStateFlow(DueCounts())
    override val dueCounts: StateFlow<DueCounts> = _dueCounts.asStateFlow()

    private val notes = mutableListOf<Note>()
    private val cards = mutableListOf<Card>()

    /** The server's count of cards introduced today, and the start of the day it was counted for. */
    private var newCardsAtLoad = 0
    private var loadedDayStart = 0L

    /** When cards were introduced since the last load, so the daily limit stays current without refetching. */
    private val introducedSinceLoad = mutableListOf<Long>()

    override suspend fun refreshDecks() {
        val dayStart = startOfDay(System.currentTimeMillis())
        val data = api.getFlashcards(zone.id).bodyOrThrow()
        mutex.withLock {
            notes.clear()
            notes += data.notes
            cards.clear()
            cards += data.cards
            newCardsAtLoad = data.newCardsToday
            loadedDayStart = dayStart
            introducedSinceLoad.clear()
            _decks.value = data.decks
            _reviewDays.value = data.reviewDays
                .groupBy { it.deckId }
                .mapValues { (_, days) -> days.associate { LocalDate.parse(it.date) to it.count } }
            updateDueCounts()
        }
    }

    override suspend fun getNotes(deckId: String): List<Note> = mutex.withLock {
        deck(deckId)
        notes.filter { it.deckId == deckId }
    }

    override suspend fun createNote(deckId: String, params: NoteParams): Note {
        val result = api.createNote(deckId, params.validated()).bodyOrThrow()
        mutex.withLock {
            store(result)
            updateDueCounts()
        }
        return result.note
    }

    override suspend fun updateNote(id: String, params: NoteParams): Note {
        val result = api.updateNote(id, params.validated()).bodyOrThrow()
        mutex.withLock {
            store(result)
            updateDueCounts()
        }
        return result.note
    }

    override suspend fun deleteNote(id: String) {
        api.deleteNote(id).checkSuccess()
        mutex.withLock {
            removeNote(id)
            updateDueCounts()
        }
    }

    override suspend fun getDueCards(deckId: String?, now: Long): List<StudyCard> = mutex.withLock {
        dueCards(candidates(deckId), now).map(::toStudyCard)
    }

    override suspend fun getCardsAhead(deckId: String?, now: Long): List<StudyCard> = mutex.withLock {
        val candidates = candidates(deckId)
        val due = dueCards(candidates, now).map { it.publicId }.toSet()
        // Started cards first, then new ones, so cards already being learned come back before fresh ones
        candidates
            .filter { it.publicId !in due }
            .sortedWith(compareBy({ it.state == CardState.NEW }, { it.due }))
            .map(::toStudyCard)
    }

    override suspend fun answerCard(cardId: String, rating: Rating, now: Long): Card {
        val updated = api.answerCard(cardId, AnswerParams(rating)).bodyOrThrow()
        mutex.withLock {
            val before = cards.find { it.publicId == cardId }
            if (before?.state == CardState.NEW) introducedSinceLoad += now
            cards.replaceAll { if (it.publicId == cardId) updated else it }
            // Count the answer on today's square without refetching
            notes.find { it.publicId == updated.noteId }?.deckId?.let { deckId ->
                val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                val days = _reviewDays.value[deckId].orEmpty()
                _reviewDays.value += deckId to (days + (today to (days[today] ?: 0) + 1))
            }
            updateDueCounts()
        }
        return updated
    }

    override fun clear() {
        // The next user starts fresh
        notes.clear()
        cards.clear()
        newCardsAtLoad = 0
        loadedDayStart = 0L
        introducedSinceLoad.clear()
        _decks.value = emptyList()
        _dueCounts.value = DueCounts()
        _reviewDays.value = emptyMap()
    }

    // Callers hold the mutex, except clear, which empties everything at once

    private fun updateDueCounts(now: Long = System.currentTimeMillis()) {
        _dueCounts.value = DueCounts(
            byDeck = _decks.value.associate { it.publicId to dueCards(candidates(it.publicId), now).size },
            total = dueCards(candidates(null), now).size
        )
    }

    /** The deck's cards, or every deck's when [deckId] is null. */
    private fun candidates(deckId: String?): List<Card> {
        val deckOf = notes.associate { it.publicId to it.deckId }
        return cards.filter { card ->
            val cardDeck = deckOf[card.noteId] ?: return@filter false
            deckId == null || cardDeck == deckId
        }
    }

    private fun dueCards(candidates: List<Card>, now: Long): List<Card> {
        val started = candidates
            .filter {
                when (it.state) {
                    CardState.NEW -> false
                    CardState.LEARNING, CardState.RELEARNING -> it.due <= now + StudyConfig.LEARN_AHEAD_MS
                    CardState.REVIEW -> it.due <= now
                }
            }
            .sortedBy { it.due }

        val startOfToday = startOfDay(now)
        // The server's count only applies while it's still the day it was counted for
        val introducedToday = (if (loadedDayStart == startOfToday) newCardsAtLoad else 0) +
                introducedSinceLoad.count { it >= startOfToday }
        val newLimit = (StudyConfig.NEW_CARDS_PER_DAY - introducedToday).coerceAtLeast(0)
        val new = candidates
            .filter { it.state == CardState.NEW }
            .sortedBy { it.due }
            .take(newLimit)
        return started + new
    }

    private fun toStudyCard(card: Card): StudyCard {
        val note = notes.first { it.publicId == card.noteId }
        val deckName = _decks.value.find { it.publicId == note.deckId }?.name ?: ""
        return StudyCard(card, note, deckName)
    }

    /** Replaces the note and its cards with what the server returned. */
    private fun store(result: NoteResult) {
        val id = result.note.publicId
        if (notes.any { it.publicId == id }) notes.replaceAll { if (it.publicId == id) result.note else it } else notes += result.note
        // Drops a reverse card the server deleted when reversible was turned off
        cards.removeAll { it.noteId == id }
        cards += result.cards
    }

    private fun deck(id: String) = _decks.value.find { it.publicId == id } ?: throw IllegalArgumentException("Deck not found")

    private fun removeNote(noteId: String) {
        cards.removeAll { it.noteId == noteId }
        notes.removeAll { it.publicId == noteId }
    }

    private fun startOfDay(now: Long) =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

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

    private fun NoteParams.validated(): NoteParams {
        val front = front.trim()
        val back = back.trim()
        require(front.isNotEmpty()) { "Front is required" }
        require(back.isNotEmpty()) { "Back is required" }
        return NoteParams(front, back, reversible)
    }
}
