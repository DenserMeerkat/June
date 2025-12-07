package com.example.june.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.june.core.domain.NoteRepo
import com.example.june.core.domain.data_classes.Note
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.screens.note.NoteAction
import com.example.june.core.presentation.screens.note.NoteState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteVM(
    savedStateHandle: SavedStateHandle,
    private val noteRepo: NoteRepo,
    private val navigator: AppNavigator
) : ViewModel() {

    private val routeArgs = savedStateHandle.toRoute<Route.Note>()
    private val currentNoteId = routeArgs.noteId

    private var existingNote: Note? = null

    private val _state = MutableStateFlow(NoteState())
    val state = _state.asStateFlow()

    init {
        if (currentNoteId != null) {
            loadNote(currentNoteId)
        }
    }

    fun onAction(action: NoteAction) {
        when (action) {
            is NoteAction.ChangeTitle -> {
                _state.update { it.copy(title = action.title) }
            }
            is NoteAction.ChangeContent -> {
                _state.update { it.copy(content = action.content) }
            }
            is NoteAction.ChangeDateTime -> {
                _state.update { it.copy(dateTime = action.dateTime) }
            }
            is NoteAction.ChangeCoverImageUri -> {
                _state.update { it.copy(coverImageUri = action.uri) }
            }
            is NoteAction.SaveNote -> {
                saveNote()
            }
            is NoteAction.NavigateBack -> {
                saveNote()
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val note = noteRepo.getNoteById(id)

            if (note != null) {
                existingNote = note

                _state.update {
                    it.copy(
                        title = note.title,
                        content = note.content,
                        coverImageUri = note.coverImageUri,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt,
                        dateTime = note.dateTime,
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveNote() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.title.isBlank() && currentState.content.isBlank()) {
                navigator.navigateBack()
                return@launch
            }

            val currentTime = System.currentTimeMillis()

            if (currentNoteId != null && existingNote != null) {
                val hasChanged = existingNote!!.title != currentState.title ||
                        existingNote!!.content != currentState.content ||
                        existingNote!!.coverImageUri != currentState.coverImageUri ||
                        existingNote!!.dateTime != currentState.dateTime

                if (hasChanged) {
                    val updatedNote = existingNote!!.copy(
                        title = currentState.title,
                        content = currentState.content,
                        coverImageUri = currentState.coverImageUri,
                        dateTime = currentState.dateTime,
                        updatedAt = currentTime
                    )
                    noteRepo.updateNote(updatedNote)
                }
            }

            else {
                val newNote = Note(
                    id = 0L,
                    title = currentState.title,
                    content = currentState.content,
                    coverImageUri = currentState.coverImageUri,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    dateTime = currentState.dateTime
                )
                noteRepo.insertNote(newNote)
            }

            navigator.navigateBack()
        }
    }
}