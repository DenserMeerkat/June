package com.denser.june.presentation.screens.home.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.model.Journal
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JournalsVM(
    private val journalRepo: JournalRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(JournalListTab.Journals)
    val selectedTab = _selectedTab.asStateFlow()

    val journals: StateFlow<List<Journal>?> = journalRepo.getJournals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val nonDraftJournals = journals.map { list ->
        list?.filter { !it.isDraft }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookmarkedJournals = journals.map { list ->
        list?.filter { it.isBookmarked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val draftJournals = journals.map { list ->
        list?.filter { it.isDraft }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onTabSelected(tab: JournalListTab) {
        _selectedTab.value = tab
    }

    fun deleteJournal(id: String) {
        viewModelScope.launch { journalRepo.softDeleteJournal(id) }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { journalRepo.toggleBookmark(id) }
    }
}