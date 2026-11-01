package com.denser.june.presentation.screens.settings.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.presentation.screens.settings.screens.lock.LockSessionVM
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultVM(
    private val journalRepository: JournalRepository,
    private val lockSessionVM: LockSessionVM
) : ViewModel() {

    val isVaultUnlocked: StateFlow<Boolean> = lockSessionVM.isVaultUnlocked

    val lockedJournals: StateFlow<List<Journal>> = journalRepository.getLockedJournals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun unlockVault() {
        lockSessionVM.unlockVault()
    }

    fun lockVault() {
        lockSessionVM.lockVault()
    }

    fun toggleBookmark(journalId: String) {
        viewModelScope.launch {
            journalRepository.toggleBookmark(journalId)
        }
    }

    fun unlockJournal(journalId: String) {
        viewModelScope.launch {
            journalRepository.toggleLock(journalId)
        }
    }

    fun deleteJournal(journalId: String) {
        viewModelScope.launch {
            journalRepository.softDeleteJournal(journalId)
        }
    }
}
