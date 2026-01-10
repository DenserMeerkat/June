package com.denser.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.data.repository.JournalRepository
import com.denser.june.core.domain.data_classes.Journal
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JournalsVM(
    private val journalRepo: JournalRepository
) : ViewModel() {

    val journals: StateFlow<List<Journal>> = journalRepo.getJournals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteJournal(id: Long) {
        viewModelScope.launch { journalRepo.deleteJournal(id) }
    }

    fun toggleBookmark(id: Long) {
        viewModelScope.launch {
            val journal = journalRepo.getJournalById(id)
            journal?.let {
                journalRepo.updateJournal(it.copy(isBookmarked = !it.isBookmarked))
            }
        }
    }
}