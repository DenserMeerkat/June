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
            is JournalAction.ChangeTitle -> _state.update {
                val newState = it.copy(title = action.title)
                if (isDirtyCheck(newState)) saveDraft(newState)
                newState.copy(isDirty = isDirtyCheck(newState))
            }
            is JournalAction.ChangeContent -> _state.update {
                val newState = it.copy(content = action.content)
                if (isDirtyCheck(newState)) saveDraft(newState)
                newState.copy(isDirty = isDirtyCheck(newState))
            }
            is JournalAction.ChangeDateTime -> _state.update {
                val newState = it.copy(dateTime = action.dateTime)
                if (isDirtyCheck(newState)) saveDraft(newState)
                newState.copy(isDirty = isDirtyCheck(newState))
            }
            is JournalAction.ChangeCoverImageUri -> _state.update {
                val newState = it.copy(coverImageUri = action.uri)
                if (isDirtyCheck(newState)) saveDraft(newState)
                newState.copy(isDirty = isDirtyCheck(newState))
            }
            is JournalAction.ToggleBookmark -> toggleBookmark()
            is JournalAction.ToggleArchive -> toggleArchive()

            is JournalAction.SaveJournal -> saveJournal()
            is JournalAction.NavigateBack -> navigator.navigateBack()
            is JournalAction.DeleteJournal -> deleteJournal()
        }
    }

    private fun isDirtyCheck(currentState: JournalState): Boolean {
        val original = existingJournal ?: return true

        return original.title != currentState.title ||
                original.content != currentState.content ||
                original.coverImageUri != currentState.coverImageUri ||
                original.dateTime != currentState.dateTime
    }

    private fun toggleBookmark() {
        viewModelScope.launch {
            val currentState = _state.value
            val newBookmarkState = !currentState.isBookmarked

            _state.update { it.copy(isBookmarked = newBookmarkState) }

            if (existingJournal != null) {
                val updatedJournal = existingJournal!!.copy(
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

            if (existingJournal != null) {
                val updatedJournal = existingJournal!!.copy(
                    isArchived = newArchiveState,
                    updatedAt = System.currentTimeMillis()
                )
                journalRepo.updateJournal(updatedJournal)
                existingJournal = updatedJournal

                if (newArchiveState) {
                    navigator.navigateBack()
                }
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
                        coverImageUri = journal.coverImageUri,
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
            if (existingJournal != null && !existingJournal!!.isDraft) {
                return@launch
            }

            if (!(currentState.title.isBlank() && currentState.content.isBlank())) {

                val currentTime = System.currentTimeMillis()
                val isNewEntry = existingJournal == null

                val journalToSave = Journal(
                    id = existingJournal?.id ?: 0L,
                    title = currentState.title,
                    content = currentState.content,
                    coverImageUri = currentState.coverImageUri,
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
    }

    private fun saveJournal() {
        viewModelScope.launch {
            val currentState = _state.value

            val currentTime = System.currentTimeMillis()

            val journalToSave = Journal(
                id = existingJournal?.id ?: 0L,
                title = currentState.title,
                content = currentState.content,
                coverImageUri = currentState.coverImageUri,
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
            if (existingJournal != null) {
                journalRepo.deleteJournal(existingJournal!!.id)
            }
            navigator.navigateBack()
        }
    }
}