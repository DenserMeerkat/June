package com.denser.june.presentation.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashVM(
    private val repository: JournalRepository
) : ViewModel() {

    val deletedJournals: StateFlow<List<Journal>> = repository.getDeletedJournals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreJournal(id: String) {
        viewModelScope.launch {
            repository.restoreJournal(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }
}
