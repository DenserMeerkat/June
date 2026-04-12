package com.denser.june.core.domain.repository

import com.denser.june.core.domain.model.Journal
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getJournals(
        query: String? = null,
        isBookmarked: Boolean? = null,
        isDraft: Boolean? = null,
        hasLocation: Boolean? = null,
        hasSong: Boolean? = null,
        hasMedia: Boolean? = null
    ): Flow<List<Journal>>

    suspend fun getAllJournals(): List<Journal>
    suspend fun getJournalById(id: String): Journal?
    suspend fun getLatestJournal(): Journal?
    fun getJournalsByDateRange(startDate: Long, endDate: Long): Flow<List<Journal>>
    fun getJournalsByMultipleTags(tags: List<String>): Flow<List<Journal>>
    
    suspend fun insertJournal(journal: Journal): String
    suspend fun softDeleteJournal(id: String)
    suspend fun restoreJournal(id: String)
    suspend fun hardDeleteJournal(id: String)
    suspend fun deleteAllJournals()
    suspend fun emptyBin()
    suspend fun restoreAllJournals()
    suspend fun updateJournal(journal: Journal)

    fun getDeletedJournals(): Flow<List<Journal>>
    suspend fun getJournalsToSync(threshold: Long): List<Journal>
    suspend fun getAllJournalsIncludeDeletedSync(): List<Journal>
    suspend fun getOldDeletedJournals(threshold: Long): List<Journal>
    
    suspend fun updateSyncStatus(id: String, cloudId: String, syncedAt: Long)

    suspend fun getAllTombstones(): List<String>
    suspend fun deleteTombstone(id: String)

    fun getTagSuggestions(query: String): Flow<List<String>>
    fun getUniqueTags(): Flow<List<String>>
    fun getTagCounts(): Flow<Map<String, Int>>
    suspend fun toggleBookmark(id: String)
    suspend fun renameTag(oldName: String, newName: String)
    suspend fun deleteTag(tagName: String)

    fun observeHasUnsyncedJournals(threshold: Long): Flow<Boolean>
    suspend fun hasUnsyncedJournals(threshold: Long): Boolean
    fun observeHasTombstones(): Flow<Boolean>
    suspend fun hasTombstones(): Boolean
}
