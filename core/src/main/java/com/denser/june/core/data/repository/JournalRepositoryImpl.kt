package com.denser.june.core.data.repository

import android.content.Context
import com.denser.june.core.data.database.journal.JournalDao
import com.denser.june.core.data.database.journal.TagCount
import com.denser.june.core.data.database.journal.TagEntity
import com.denser.june.core.data.database.journal.JournalTagCrossRef
import com.denser.june.core.data.database.journal.DeletedJournalTombstone
import com.denser.june.core.data.mappers.asDomain
import com.denser.june.core.data.mappers.asEntity
import com.denser.june.core.data.sync.SyncWorker
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.preferences.SyncPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class JournalRepositoryImpl(
    private val journalDao: JournalDao,
    private val syncPrefs: SyncPreferences,
    private val context: Context
) : JournalRepository {

    override fun getJournals(
        query: String?,
        isBookmarked: Boolean?,
        isDraft: Boolean?,
        hasLocation: Boolean?,
        hasSong: Boolean?,
        hasMedia: Boolean?
    ): Flow<List<Journal>> {
        return journalDao.getJournals(
            query = query,
            isBookmarked = isBookmarked,
            isDraft = isDraft,
            hasLocation = hasLocation,
            hasSong = hasSong,
            hasMedia = hasMedia
        ).map { it.asDomain() }
    }

    override suspend fun getAllJournals(): List<Journal> {
        return journalDao.getAllJournalsSync().asDomain()
    }

    override suspend fun getJournalById(id: String): Journal? {
        return journalDao.getJournalById(id)?.asDomain()
    }

    override suspend fun getLatestJournal(): Journal? {
        return journalDao.getLatestJournal()?.asDomain()
    }

    override fun getJournalsByDateRange(startDate: Long, endDate: Long): Flow<List<Journal>> {
        return journalDao.getJournalsByDateRange(startDate, endDate).map { it.asDomain() }
    }

    override fun getJournalsByMultipleTags(tags: List<String>): Flow<List<Journal>> {
        return journalDao.getJournalsWithAllTags(tags = tags, tagCount = tags.size).map { it.asDomain() }
    }

    override suspend fun insertJournal(journal: Journal): String {
        val journalToInsert = if (journal.id.isBlank()) {
            journal.copy(id = java.util.UUID.randomUUID().toString())
        } else journal
        
        journalDao.insertJournal(journalToInsert.asEntity())
        updateJournalTags(journalToInsert)
        triggerSync()
        return journalToInsert.id
    }

    override suspend fun softDeleteJournal(id: String) {
        journalDao.softDeleteJournal(id, System.currentTimeMillis())
        triggerSync()
    }

    override suspend fun restoreJournal(id: String) {
        val journal = journalDao.getJournalById(id) ?: return
        journalDao.updateJournal(journal.copy(deletedAt = null, updatedAt = System.currentTimeMillis()))
        triggerSync()
    }

    override suspend fun hardDeleteJournal(id: String) {
        journalDao.insertTombstone(DeletedJournalTombstone(id))
        journalDao.hardDeleteJournal(id)
        triggerSync()
    }

    override suspend fun deleteAllJournals() {
        journalDao.softDeleteAllJournals(System.currentTimeMillis())
        triggerSync()
    }

    override suspend fun emptyBin() {
        val deletedIds = journalDao.getDeletedJournalsSync().map { it.id }
        if (deletedIds.isNotEmpty()) {
            journalDao.insertTombstones(deletedIds.map { DeletedJournalTombstone(it) })
            journalDao.emptyBin()
            triggerSync()
        }
    }

    override suspend fun restoreAllJournals() {
        journalDao.restoreAllJournals(System.currentTimeMillis())
        triggerSync()
    }

    override suspend fun updateJournal(journal: Journal) {
        journalDao.updateJournal(journal.asEntity())
        updateJournalTags(journal)
        triggerSync()
    }

    private suspend fun updateJournalTags(journal: Journal) {
        journalDao.deleteTagsForJournal(journal.id)
        journal.tags.forEach { tagName ->
            val trimmedName = tagName.trim()
            if (trimmedName.isNotBlank()) {
                var tagId = journalDao.getTagIdByName(trimmedName)
                if (tagId == null) {
                    tagId = journalDao.insertTag(TagEntity(name = trimmedName))
                }
                if (tagId == -1L) {
                    tagId = journalDao.getTagIdByName(trimmedName)
                }
                if (tagId != null && tagId != -1L) {
                    journalDao.insertJournalTagCrossRef(JournalTagCrossRef(id = journal.id, tagId = tagId))
                }
            }
        }
        journalDao.deleteOrphanedTags()
    }

    private suspend fun triggerSync() {
        if (syncPrefs.getSyncEnabled().first() && syncPrefs.isAutomaticSyncEnabled().first()) {
            val onlyWifi = syncPrefs.getSyncOnlyOnWifi().first()
            SyncWorker.enqueue(context, onlyWifi)
        }
    }

    override fun getTagSuggestions(query: String): Flow<List<String>> {
        return journalDao.getTagSuggestions(query)
    }

    override fun getUniqueTags(): Flow<List<String>> {
        return journalDao.getAllUniqueTags()
    }

    override fun getTagCounts(): Flow<Map<String, Int>> {
        return journalDao.getAllTagCounts().map { list ->
            list.associate { it.name to it.count }
        }
    }

    override suspend fun renameTag(oldName: String, newName: String) {
        journalDao.updateTagName(oldName, newName)
        journalDao.bumpJournalTimestampsByTag(newName, System.currentTimeMillis())
        triggerSync()
    }

    override suspend fun deleteTag(tagName: String) {
        journalDao.bumpJournalTimestampsByTag(tagName, System.currentTimeMillis())
        journalDao.deleteTag(tagName)
        triggerSync()
    }

    override suspend fun toggleBookmark(id: String) {
        val journal = journalDao.getJournalById(id) ?: return
        journalDao.updateJournal(journal.copy(
            isBookmarked = !journal.isBookmarked,
            updatedAt = System.currentTimeMillis()
        ))
        triggerSync()
    }

    override fun getDeletedJournals(): Flow<List<Journal>> {
        return journalDao.getDeletedJournals().map { it.asDomain() }
    }

    override suspend fun getJournalsToSync(threshold: Long): List<Journal> {
        return journalDao.getJournalsToSync(threshold).asDomain()
    }

    override suspend fun updateSyncStatus(id: String, cloudId: String, syncedAt: Long) {
        journalDao.updateSyncStatus(id, cloudId, syncedAt)
    }

    override suspend fun getAllJournalsIncludeDeletedSync(): List<Journal> {
        return journalDao.getAllJournalsIncludeDeletedSync().asDomain()
    }

    override suspend fun getOldDeletedJournals(threshold: Long): List<Journal> {
        return journalDao.getOldDeletedJournals(threshold).asDomain()
    }

    override suspend fun getAllTombstones(): List<String> {
        return journalDao.getAllTombstones().map { it.id }
    }

    override suspend fun deleteTombstone(id: String) {
        journalDao.deleteTombstone(id)
    }

    override fun observeHasUnsyncedJournals(threshold: Long): Flow<Boolean> {
        return journalDao.observeHasUnsyncedJournals(threshold)
    }

    override suspend fun hasUnsyncedJournals(threshold: Long): Boolean {
        return journalDao.hasUnsyncedJournals(threshold)
    }

    override fun observeHasTombstones(): Flow<Boolean> {
        return journalDao.observeHasTombstones()
    }

    override suspend fun hasTombstones(): Boolean {
        return journalDao.hasTombstones()
    }
}
