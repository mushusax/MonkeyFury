package com.mushusax.monkeyfury.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mushusax.monkeyfury.MonkeyFuryApplication
import com.mushusax.monkeyfury.data.MediaStatus
import com.mushusax.monkeyfury.data.User
import com.mushusax.monkeyfury.data.repository.ActivityRepository
import com.mushusax.monkeyfury.data.repository.FlashcardRepository
import com.mushusax.monkeyfury.data.repository.MediaRepository
import com.mushusax.monkeyfury.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
    private val flashcardRepository: FlashcardRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.currentUser

    /** Cards due across every deck, for the Flashcard button's badge. */
    val dueCardCount: StateFlow<Int> = flashcardRepository.dueCounts
        .map { it.total }
        .stateIn(viewModelScope, SharingStarted.Eagerly, flashcardRepository.dueCounts.value.total)

    /** Books, anime and movies in progress, for the Media button's badge. */
    val inProgressMediaCount: StateFlow<Int> = mediaRepository.media
        .map { list -> list.count { it.status == MediaStatus.IN_PROGRESS } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        viewModelScope.launch {
            try {
                flashcardRepository.refreshDecks()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The badge is a hint; the Flashcard screen loads again and handles failures
            }
        }
        viewModelScope.launch {
            try {
                mediaRepository.refreshMedia()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Same as above: the Media screen loads again and handles failures
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            userRepository.logout()
            activityRepository.clear()
            flashcardRepository.clear()
            mediaRepository.clear()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MonkeyFuryApplication
                DashboardViewModel(
                    userRepository = app.userRepository,
                    activityRepository = app.activityRepository,
                    flashcardRepository = app.flashcardRepository,
                    mediaRepository = app.mediaRepository
                )
            }
        }
    }
}