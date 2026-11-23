package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Card
import com.mushusax.monkeyfury.data.Deck
import com.mushusax.monkeyfury.data.DeckParams
import com.mushusax.monkeyfury.data.Note
import com.mushusax.monkeyfury.data.NoteParams
import com.mushusax.monkeyfury.data.Rating
import com.mushusax.monkeyfury.data.StudyCard
import kotlinx.coroutines.flow.StateFlow

/** Decks, notes and the cards studied from them. Unknown ids and blank text throw IllegalArgumentException. */
interface FlashcardRepository {
    /** The latest deck list, shared by every screen; updated by [refreshDecks] and edits. */
    val decks: StateFlow<List<Deck>>

    suspend fun refreshDecks()
    suspend fun createDeck(params: DeckParams): Deck
    suspend fun updateDeck(id: String, params: DeckParams): Deck
    /** Deletes the deck with its notes, cards and review history. */
    suspend fun deleteDeck(id: String)

    suspend fun getNotes(deckId: String): List<Note>
    /** Creates the note's card, plus a reverse card when the note is reversible. */
    suspend fun createNote(deckId: String, params: NoteParams): Note
    /** Keeps existing schedules. Turning reversible on adds a new reverse card; turning it off deletes it. */
    suspend fun updateNote(id: String, params: NoteParams): Note
    suspend fun deleteNote(id: String)

    /**
     * Cards to study at [now]: every started card that is due, soonest first, then never-studied
     * cards up to what's left of today's new-card limit. [deckId] null means every deck.
     */
    suspend fun getDueCards(deckId: String?, now: Long = System.currentTimeMillis()): List<StudyCard>

    /** Records an answer and reschedules the card. */
    suspend fun answerCard(cardId: String, rating: Rating, now: Long = System.currentTimeMillis()): Card

    /** Forgets cached data, e.g. on logout, so the next user never sees it. */
    fun clear()
}
