package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Card
import com.mushusax.monkeyfury.data.CardDirection
import com.mushusax.monkeyfury.data.CardState
import com.mushusax.monkeyfury.data.Deck
import com.mushusax.monkeyfury.data.DeckParams
import com.mushusax.monkeyfury.data.FsrsConfig
import com.mushusax.monkeyfury.data.FsrsScheduler
import com.mushusax.monkeyfury.data.Note
import com.mushusax.monkeyfury.data.NoteParams
import com.mushusax.monkeyfury.data.Rating
import com.mushusax.monkeyfury.data.ReviewLog
import com.mushusax.monkeyfury.data.StudyCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * TEMPORARY: until the flashcard endpoints exist, everything lives in memory, starting empty.
 * Changes are lost when the app process ends. Replace the bodies with MonkeyFuryApi calls
 * once the endpoints are in place.
 */
class NetworkFlashcardRepository(
    private val scheduler: FsrsScheduler = FsrsScheduler(),
    private val zone: ZoneId = ZoneId.systemDefault()
) : FlashcardRepository {

    private val mutex = Mutex()

    private val _decks = MutableStateFlow<List<Deck>>(emptyList())
    override val decks: StateFlow<List<Deck>> = _decks.asStateFlow()

    private val notes = mutableListOf<Note>()
    private val cards = mutableListOf<Card>()
    private val reviewLogs = mutableListOf<ReviewLog>()

    override suspend fun refreshDecks() {
        // Nothing to fetch while the data is in memory
    }

    override suspend fun createDeck(params: DeckParams): Deck = mutex.withLock {
        val now = System.currentTimeMillis()
        val p = params.validated()
        val deck = Deck(newId(), p.name, p.description, createdAt = now, updatedAt = now)
        _decks.value += deck
        deck
    }

    override suspend fun updateDeck(id: String, params: DeckParams): Deck = mutex.withLock {
        val p = params.validated()
        val updated = deck(id).copy(name = p.name, description = p.description, updatedAt = System.currentTimeMillis())
        _decks.value = _decks.value.map { if (it.publicId == id) updated else it }
        updated
    }

    override suspend fun deleteDeck(id: String) = mutex.withLock {
        deck(id)
        notes.filter { it.deckId == id }.forEach { removeNote(it.publicId) }
        _decks.value = _decks.value.filterNot { it.publicId == id }
    }

    override suspend fun getNotes(deckId: String): List<Note> = mutex.withLock {
        deck(deckId)
        notes.filter { it.deckId == deckId }
    }

    override suspend fun createNote(deckId: String, params: NoteParams): Note = mutex.withLock {
        deck(deckId)
        addNote(deckId, params.validated(), System.currentTimeMillis())
    }

    override suspend fun updateNote(id: String, params: NoteParams): Note = mutex.withLock {
        val p = params.validated()
        val now = System.currentTimeMillis()
        val updated = note(id).copy(front = p.front, back = p.back, reversible = p.reversible, updatedAt = now)
        notes.replaceAll { if (it.publicId == id) updated else it }

        val reverse = cards.find { it.noteId == id && it.direction == CardDirection.REVERSE }
        if (p.reversible && reverse == null) {
            cards += newCard(id, CardDirection.REVERSE, now)
        } else if (!p.reversible && reverse != null) {
            removeCard(reverse.publicId)
        }
        updated
    }

    override suspend fun deleteNote(id: String) = mutex.withLock {
        note(id)
        removeNote(id)
    }

    override suspend fun getDueCards(deckId: String?, now: Long): List<StudyCard> = mutex.withLock {
        val deckNames = _decks.value.associate { it.publicId to it.name }
        val notesById = notes.associateBy { it.publicId }
        val candidates = cards.filter { card ->
            val note = notesById[card.noteId] ?: return@filter false
            deckId == null || note.deckId == deckId
        }

        val started = candidates
            .filter { it.state != CardState.NEW && it.due <= now }
            .sortedBy { it.due }

        val startOfToday = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val introducedToday = reviewLogs.count { it.stateBefore == CardState.NEW && it.reviewedAt >= startOfToday }
        val newLimit = (FsrsConfig.NEW_CARDS_PER_DAY - introducedToday).coerceAtLeast(0)
        val new = candidates
            .filter { it.state == CardState.NEW }
            .sortedBy { it.due }
            .take(newLimit)

        (started + new).map { card ->
            val note = notesById.getValue(card.noteId)
            StudyCard(card, note, deckNames[note.deckId] ?: "")
        }
    }

    override suspend fun answerCard(cardId: String, rating: Rating, now: Long): Card = mutex.withLock {
        val card = cards.find { it.publicId == cardId } ?: throw IllegalArgumentException("Card not found")
        val (updated, log) = scheduler.review(card, rating, now)
        cards.replaceAll { if (it.publicId == cardId) updated else it }
        reviewLogs += log
        updated
    }

    override fun clear() {
        // The next user starts fresh
        notes.clear()
        cards.clear()
        reviewLogs.clear()
        _decks.value = emptyList()
    }

    // Callers hold the mutex, except clear, which empties everything at once

    private fun deck(id: String) = _decks.value.find { it.publicId == id } ?: throw IllegalArgumentException("Deck not found")

    private fun note(id: String) = notes.find { it.publicId == id } ?: throw IllegalArgumentException("Note not found")

    private fun addNote(deckId: String, params: NoteParams, now: Long): Note {
        val note = Note(newId(), deckId, params.front, params.back, params.reversible, createdAt = now, updatedAt = now)
        notes += note
        cards += newCard(note.publicId, CardDirection.FORWARD, now)
        if (params.reversible) cards += newCard(note.publicId, CardDirection.REVERSE, now)
        return note
    }

    private fun newCard(noteId: String, direction: CardDirection, now: Long) =
        Card(publicId = newId(), noteId = noteId, direction = direction, due = now, updatedAt = now)

    private fun removeNote(noteId: String) {
        cards.filter { it.noteId == noteId }.forEach { removeCard(it.publicId) }
        notes.removeAll { it.publicId == noteId }
    }

    private fun removeCard(cardId: String) {
        reviewLogs.removeAll { it.cardId == cardId }
        cards.removeAll { it.publicId == cardId }
    }

    private fun newId() = UUID.randomUUID().toString()

    private fun DeckParams.validated(): DeckParams {
        val name = name.trim()
        require(name.isNotEmpty()) { "Deck name is required" }
        return DeckParams(name, description.trim())
    }

    private fun NoteParams.validated(): NoteParams {
        val front = front.trim()
        val back = back.trim()
        require(front.isNotEmpty()) { "Front is required" }
        require(back.isNotEmpty()) { "Back is required" }
        return NoteParams(front, back, reversible)
    }
}
