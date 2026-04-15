package com.denser.june.presentation.screens.settings.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.preferences.JournalPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BinVM(
    private val repository: JournalRepository,
    private val journalPrefs: JournalPreferences
) : ViewModel() {

    val timeFormat: StateFlow<TimeFormat> = journalPrefs.timeFormat()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimeFormat.TWELVE_HOUR
        )

    val deletedJournals: StateFlow<List<Journal>> = repository.getDeletedJournals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreJournal(id: String) {
        viewModelScope.launch {
            repository.restoreJournal(id)
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            repository.emptyBin()
        }
    }

    fun restoreAll() {
        viewModelScope.launch {
            repository.restoreAllJournals()
        }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { repository.toggleBookmark(id) }
    }

    fun deletePermanently(id: String) {
        viewModelScope.launch {
            repository.hardDeleteJournal(id)
        }
    }
}