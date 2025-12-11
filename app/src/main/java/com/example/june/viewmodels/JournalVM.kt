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
    private val currentJournalId = routeArgs.journalId

    private var existingJournal: Journal? = null

    private val _state = MutableStateFlow(JournalState())
    val state = _state.asStateFlow()

    init {
        if (currentJournalId != null) {
            loadJournal(currentJournalId)
        }
    }

    fun onAction(action: JournalAction) {
        when (action) {
            is JournalAction.ChangeTitle -> _state.update { it.copy(title = action.title) }
            is JournalAction.ChangeContent -> _state.update { it.copy(content = action.content) }
            is JournalAction.ChangeDateTime -> _state.update { it.copy(dateTime = action.dateTime) }
            is JournalAction.ChangeCoverImageUri -> _state.update { it.copy(coverImageUri = action.uri) }
            is JournalAction.SaveJournal -> saveJournal()
            is JournalAction.NavigateBack -> navigator.navigateBack()
            is JournalAction.DeleteJournal -> deleteJournal()
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val currentState = _state.value

        if (currentJournalId == null) {
            return currentState.title.isNotBlank() || currentState.content.isNotBlank()
        }

        return existingJournal?.let { original ->
            original.title != currentState.title ||
                    original.content != currentState.content ||
                    original.coverImageUri != currentState.coverImageUri ||
                    original.dateTime != currentState.dateTime
        } ?: false
    }

    private fun loadJournal(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val journal = journalRepo.getJournalById(id)

            if (journal != null) {
                existingJournal = journal
                _state.update {
                    it.copy(
                        title = journal.title,
                        content = journal.content,
                        coverImageUri = journal.coverImageUri,
                        createdAt = journal.createdAt,
                        updatedAt = journal.updatedAt,
                        dateTime = journal.dateTime,
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveJournal() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.title.isBlank() && currentState.content.isBlank()) {
                navigator.navigateBack()
                return@launch
            }

            val currentTime = System.currentTimeMillis()

            if (currentJournalId != null && existingJournal != null) {
                val updatedJournal = existingJournal!!.copy(
                    title = currentState.title,
                    content = currentState.content,
                    coverImageUri = currentState.coverImageUri,
                    dateTime = currentState.dateTime,
                    updatedAt = currentTime
                )
                journalRepo.updateJournal(updatedJournal)
            } else {
                val newJournal = Journal(
                    id = 0L,
                    title = currentState.title,
                    content = currentState.content,
                    coverImageUri = currentState.coverImageUri,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    dateTime = currentState.dateTime
                )
                journalRepo.insertJournal(newJournal)
            }
            navigator.navigateBack()
        }
    }

    private fun deleteJournal() {
        viewModelScope.launch {
            if (currentJournalId != null) {
                journalRepo.deleteJournal(currentJournalId)
            }
            navigator.navigateBack()
        }
    }
}