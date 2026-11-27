package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.Deck
import com.mushusax.monkeyfury.data.Note
import com.mushusax.monkeyfury.data.NoteParams
import com.mushusax.monkeyfury.data.Rating
import com.mushusax.monkeyfury.data.StudyCard
import com.mushusax.monkeyfury.data.repository.FlashcardRepository
import com.mushusax.monkeyfury.data.repository.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Flashcard hub: which deck is selected, and editing that deck's cards in place. */
class FlashcardViewModel(private val flashcardRepository: FlashcardRepository) : ViewModel() {

    val decks: StateFlow<List<Deck>> = flashcardRepository.decks

    private val selectedDeckId = MutableStateFlow<String?>(null)

    /** The chosen deck; the first deck until one is chosen, or if the chosen one goes away. */
    val selectedDeck: StateFlow<Deck?> = combine(decks, selectedDeckId, ::deckWithId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, deckWithId(decks.value, null))

    /** True while the selected deck's cards are listed for editing, in place of the hub. */
    private val _editing = MutableStateFlow(false)
    val editing: StateFlow<Boolean> = _editing.asStateFlow()

    /** The selected deck's notes; a reversible note is studied as two cards. */
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    /**
     * The card being studied, or null when not studying.
     * TEMPORARY: always the deck's first card, until FSRS picks the cards.
     */
    private val _studyCard = MutableStateFlow<StudyCard?>(null)
    val studyCard: StateFlow<StudyCard?> = _studyCard.asStateFlow()

    /** Whether the studied card's back is showing. */
    private val _revealed = MutableStateFlow(false)
    val revealed: StateFlow<Boolean> = _revealed.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // A new ViewModel per visit, so this fetches every time the Flashcard screen is opened
        refreshDecks()
        viewModelScope.launch {
            // Reload whenever the selection changes; a newer selection cancels an older load
            selectedDeck.map { it?.publicId }.distinctUntilChanged().collectLatest { id ->
                // Never show the previous deck's cards under the new deck's name
                _notes.value = emptyList()
                if (id != null) guarded { reloadNotes(id) }
            }
        }
    }

    fun selectDeck(deck: Deck) {
        stopStudy()
        selectedDeckId.value = deck.publicId
    }

    fun startEditing() {
        stopStudy()
        _editing.value = true
    }

    /**
     * Shows the front of the deck's first card; does nothing if the deck has none. Answers aren't
     * recorded yet, so this is always the same card, until FSRS picks the cards.
     */
    fun startStudy() = run {
        val deckId = selectedDeck.value?.publicId ?: return@run
        val card = flashcardRepository.getDueCards(deckId).firstOrNull() ?: return@run
        // The deck may have changed while loading
        if (selectedDeck.value?.publicId != deckId) return@run
        _studyCard.value = card
        _revealed.value = false
    }

    fun reveal() {
        if (_studyCard.value != null) _revealed.value = true
    }

    /** Every rating ends the round for now; FSRS will schedule the card from [rating] later. */
    @Suppress("UNUSED_PARAMETER")
    fun answer(rating: Rating) = stopStudy()

    fun stopStudy() {
        _studyCard.value = null
        _revealed.value = false
    }

    fun stopEditing() {
        _editing.value = false
    }

    /** Fetches every deck, note and card from the server, then the selected deck's notes. */
    fun refreshDecks() = run {
        flashcardRepository.refreshDecks()
        // The selection may be the same deck as before the fetch, which wouldn't trigger a reload
        deckWithId(decks.value, selectedDeckId.value)?.let { reloadNotes(it.publicId) }
    }

    fun refresh() = run { selectedDeck.value?.let { reloadNotes(it.publicId) } }

    /** Creates in the selected deck when [id] is null, otherwise updates. Calls [onDone] only on success. */
    fun save(id: String?, params: NoteParams, onDone: () -> Unit) = run {
        val deckId = selectedDeck.value?.publicId ?: return@run
        if (id == null) flashcardRepository.createNote(deckId, params) else flashcardRepository.updateNote(id, params)
        reloadNotes(deckId)
        onDone()
    }

    fun delete(id: String, onDone: () -> Unit) = run {
        val deckId = selectedDeck.value?.publicId ?: return@run
        flashcardRepository.deleteNote(id)
        reloadNotes(deckId)
        onDone()
    }

    fun clearError() {
        _error.value = null
    }

    private fun deckWithId(decks: List<Deck>, id: String?): Deck? =
        decks.find { it.publicId == id } ?: decks.firstOrNull()

    private suspend fun reloadNotes(deckId: String) {
        val notes = flashcardRepository.getNotes(deckId)
        if (selectedDeck.value?.publicId == deckId) _notes.value = notes
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch { guarded(block) }
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        _loading.value = true
        _error.value = null
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = e.toUserMessage()
        } finally {
            _loading.value = false
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                FlashcardViewModel(flashcardRepository = app.flashcardRepository)
            }
        }
    }
}
