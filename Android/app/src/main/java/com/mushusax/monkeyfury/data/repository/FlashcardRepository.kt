package com.mushusax.monkeyfury.data.repository

import com.mushusax.monkeyfury.data.Card
import com.mushusax.monkeyfury.data.Deck
import com.mushusax.monkeyfury.data.DueCounts
import com.mushusax.monkeyfury.data.Note
import com.mushusax.monkeyfury.data.NoteParams
import com.mushusax.monkeyfury.data.Rating
import com.mushusax.monkeyfury.data.StudyCard
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Decks, notes and the cards studied from them. Unknown ids and blank text throw IllegalArgumentException. */
interface FlashcardRepository {
    /** The latest deck list, shared by every screen; updated by [refreshDecks]. */
    val decks: StateFlow<List<Deck>>

    /**
     * Due cards per deck and in total; updated by [refreshDecks], edits and answers. Cards that
     * come due as time passes are only counted at the next of those.
     */
    val dueCounts: StateFlow<DueCounts>

    /** Answers per day this month, by deck id; updated by [refreshDecks] and answers. */
    val reviewDays: StateFlow<Map<String, Map<LocalDate, Int>>>

    suspend fun refreshDecks()

    suspend fun getNotes(deckId: String): List<Note>
    /** Creates the note's card, plus a reverse card when the note is reversible. */
    suspend fun createNote(deckId: String, params: NoteParams): Note
    /** Keeps existing schedules. Turning reversible on adds a new reverse card; turning it off deletes it. */
    suspend fun updateNote(id: String, params: NoteParams): Note
    suspend fun deleteNote(id: String)

    /**
     * Cards to study at [now]: every started card that is due, soonest first, then never-studied
     * cards up to what's left of today's new-card limit. Learning cards due within
     * StudyConfig.LEARN_AHEAD_MS count as due. [deckId] null means every deck.
     */
    suspend fun getDueCards(deckId: String?, now: Long = System.currentTimeMillis()): List<StudyCard>

    /**
     * Every card [getDueCards] leaves out: started cards not yet due, then never-studied cards past
     * today's limit, soonest due first. For studying beyond the schedule.
     */
    suspend fun getCardsAhead(deckId: String?, now: Long = System.currentTimeMillis()): List<StudyCard>

    /** Sends the answer to the server, which reschedules the card with FSRS and logs it. */
    suspend fun answerCard(cardId: String, rating: Rating, now: Long = System.currentTimeMillis()): Card

    /** Forgets cached data, e.g. on logout, so the next user never sees it. */
    fun clear()
}
