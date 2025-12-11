package com.example.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.june.core.data.repository.JournalRepository
import com.example.june.core.domain.data_classes.Journal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeVM(
    private val journalRepo: JournalRepository
) : ViewModel() {

    val journals: StateFlow<List<Journal>> = journalRepo.getJournals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteJournal(id: Long) {
        viewModelScope.launch {
            journalRepo.deleteJournal(id)
        }
    }
}