package com.example.june.core.domain

import com.example.june.core.domain.data_classes.Journal
import kotlinx.coroutines.flow.Flow

interface JournalRepo {
    suspend fun insertJournal(journal: Journal)
    fun getJournals(): Flow<List<Journal>>
    suspend fun getAllJournals(): List<Journal>
    suspend fun getJournalById(id: Long): Journal?
    suspend fun searchJournals(query: String): Flow<List<Journal>>
    suspend fun updateJournal(journal: Journal)
    suspend fun deleteJournal(id: Long)
    suspend fun deleteAllJournals()
}