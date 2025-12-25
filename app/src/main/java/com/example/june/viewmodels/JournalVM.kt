package com.example.june.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.june.core.domain.JournalRepo
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.screens.journal.JournalAction
import com.example.june.core.presentation.screens.journal.JournalState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JournalVM(
    savedStateHandle: SavedStateHandle,
    private val journalRepo: JournalRepo,
    private val navigator: AppNavigator
) : ViewModel() {

    private val routeArgs = savedStateHandle.toRoute<Route.Journal>()
    private val initialJournalId = routeArgs.journalId

    private var existingJournal: Journal? = null

    private val _state = MutableStateFlow(JournalState())
    val state = _state.asStateFlow()

    init {
        if (initialJournalId != null) {
            loadJournal(initialJournalId)
        }
    }

    fun onAction(action: JournalAction) {
        when (action) {
            is JournalAction.ChangeTitle -> updateState { it.copy(title = action.title) }
            is JournalAction.ChangeContent -> updateState { it.copy(content = action.content) }
            is JournalAction.ChangeDateTime -> updateState { it.copy(dateTime = action.dateTime) }
            is JournalAction.AddImage -> updateState { it.copy(images = it.images + action.uri) }
            is JournalAction.RemoveImage -> updateState { it.copy(images = it.images - action.uri) }
            is JournalAction.MoveImageToFront -> {
                val currentImages = _state.value.images.toMutableList()
                if (currentImages.remove(action.uri)) {
                    currentImages.add(action.uri)
                    updateState { it.copy(images = currentImages) }
                }
            }
            is JournalAction.SetLocation -> updateState { it.copy(location = action.location) }
            is JournalAction.ToggleBookmark -> toggleBookmark()
            is JournalAction.ToggleArchive -> toggleArchive()
            is JournalAction.SaveJournal -> saveJournal()
            is JournalAction.NavigateBack -> navigator.navigateBack()
            is JournalAction.DeleteJournal -> deleteJournal()
        }
    }

    private fun updateState(update: (JournalState) -> JournalState) {
        _state.update { currentState ->
            val newState = update(currentState)
            if (isDirtyCheck(newState)) {
                saveDraft(newState)
            }
            newState.copy(isDirty = isDirtyCheck(newState))
        }
    }

    private fun isDirtyCheck(currentState: JournalState): Boolean {
        val original = existingJournal ?: return true

        return original.title != currentState.title ||
                original.content != currentState.content ||
                original.images != currentState.images ||
                original.location != currentState.location ||
                original.dateTime != currentState.dateTime
    }

    private fun toggleBookmark() {
        viewModelScope.launch {
            val currentState = _state.value
            val newBookmarkState = !currentState.isBookmarked
            _state.update { it.copy(isBookmarked = newBookmarkState) }

            existingJournal?.let {
                val updatedJournal = it.copy(
                    isBookmarked = newBookmarkState,
                    updatedAt = System.currentTimeMillis()
                )
                journalRepo.updateJournal(updatedJournal)
                existingJournal = updatedJournal
            }
        }
    }

    private fun toggleArchive() {
        viewModelScope.launch {
            val currentState = _state.value
            val newArchiveState = !currentState.isArchived
            _state.update { it.copy(isArchived = newArchiveState) }

            existingJournal?.let {
                val updatedJournal = it.copy(
                    isArchived = newArchiveState,
                    updatedAt = System.currentTimeMillis()
                )
                journalRepo.updateJournal(updatedJournal)
                existingJournal = updatedJournal
                if (newArchiveState) navigator.navigateBack()
            }
        }
    }

    private fun loadJournal(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val journal = journalRepo.getJournalById(id)

            if (journal != null) {
                existingJournal = journal
                _state.update {
                    it.copy(
                        journalId = journal.id,
                        title = journal.title,
                        content = journal.content,
                        images = journal.images,
                        location = journal.location,
                        songDetails = journal.songDetails,
                        createdAt = journal.createdAt,
                        updatedAt = journal.updatedAt,
                        dateTime = journal.dateTime,
                        isBookmarked = journal.isBookmarked,
                        isArchived = journal.isArchived,
                        isDraft = journal.isDraft,
                        isLoading = false,
                        isDirty = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveDraft(currentState: JournalState) {
        viewModelScope.launch {
            if (existingJournal != null && !existingJournal!!.isDraft) return@launch

            if (currentState.title.isBlank() && currentState.content.isBlank() && currentState.images.isEmpty()) return@launch

            val currentTime = System.currentTimeMillis()
            val isNewEntry = existingJournal == null

            val journalToSave = Journal(
                id = existingJournal?.id ?: 0L,
                title = currentState.title,
                content = currentState.content,
                images = currentState.images,
                location = currentState.location,
                songDetails = currentState.songDetails,
                createdAt = existingJournal?.createdAt ?: currentTime,
                updatedAt = currentTime,
                dateTime = currentState.dateTime,
                isBookmarked = currentState.isBookmarked,
                isArchived = currentState.isArchived,
                isDraft = true
            )

            if (isNewEntry) {
                val newId = journalRepo.insertJournal(journalToSave)
                val savedDraft = journalToSave.copy(id = newId)
                existingJournal = savedDraft
                _state.update { it.copy(journalId = newId, isDirty = false, isDraft = true) }
            } else {
                journalRepo.updateJournal(journalToSave)
                existingJournal = journalToSave
                _state.update { it.copy(isDirty = false) }
            }
        }
    }

    private fun saveJournal() {
        viewModelScope.launch {
            val currentState = _state.value
            val currentTime = System.currentTimeMillis()

            val journalToSave = Journal(
                id = existingJournal?.id ?: 0L,
                title = currentState.title,
                content = currentState.content,
                images = currentState.images,
                location = currentState.location,
                songDetails = currentState.songDetails,
                createdAt = existingJournal?.createdAt ?: currentTime,
                updatedAt = currentTime,
                dateTime = currentState.dateTime,
                isBookmarked = currentState.isBookmarked,
                isArchived = currentState.isArchived,
                isDraft = false
            )

            if (existingJournal != null) {
                journalRepo.updateJournal(journalToSave)
                existingJournal = journalToSave
            } else {
                val newId = journalRepo.insertJournal(journalToSave)
                existingJournal = journalToSave.copy(id = newId)
                _state.update { it.copy(journalId = newId) }
            }

            _state.update { it.copy(isDirty = false, isDraft = false) }
            navigator.navigateBack()
        }
    }

    private fun deleteJournal() {
        viewModelScope.launch {
            existingJournal?.let { journalRepo.deleteJournal(it.id) }
            navigator.navigateBack()
        }
    }
}