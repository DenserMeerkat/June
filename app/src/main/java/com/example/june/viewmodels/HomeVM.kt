package com.example.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.june.core.data.repository.NoteRepository
import com.example.june.core.domain.data_classes.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeVM(
    private val noteRepo: NoteRepository
) : ViewModel() {

    val notes: StateFlow<List<Note>> = noteRepo.getNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            noteRepo.deleteNote(id)
        }
    }
}