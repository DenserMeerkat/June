package com.denser.june.core.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.denser.june.core.data.database.journal.JournalDao
import com.denser.june.core.data.database.journal.JournalDatabase
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

class JournalRepositoryImpl(
    private val journalDao: JournalDao,
    private val journalDatabase: JournalDatabase? = null,
    private val syncPrefs: SyncPreferences? = null,
    private val context: Context? = null
) : JournalRepository {

    internal var transactionRunner: suspend (suspend () -> Unit) -> Unit = { block ->
        val db = journalDatabase
        if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

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

        transactionRunner {
            journalDao.insertJournal(journalToInsert.asEntity())
            replaceJournalTags(journalToInsert.id, journalToInsert.tags)
        }
        return journalToInsert.id
    }

    override suspend fun softDeleteJournal(id: String) {
        journalDao.softDeleteJournal(id, System.currentTimeMillis())
    }

    override suspend fun restoreJournal(id: String) {
        val journal = journalDao.getJournalById(id) ?: return
        journalDao.updateJournal(journal.copy(deletedAt = null, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun hardDeleteJournal(id: String) {
        transactionRunner {
            val journal = journalDao.getJournalById(id)
            if (journal?.syncedAt != null) {
                journalDao.insertTombstone(DeletedJournalTombstone(id))
            }
            // Drop this journal's cross references, otherwise the filter queries keep
            // joining against rows that no longer have a matching journal.
            journalDao.deleteTagsForJournal(id)
            journalDao.hardDeleteJournal(id)
        }
    }

    override suspend fun deleteAllJournals() {
        journalDao.softDeleteAllJournals(System.currentTimeMillis())
    }

    override suspend fun emptyBin() {
        transactionRunner {
            val deleted = journalDao.getDeletedJournalsSync()
            val syncedDeletedIds = deleted.filter { it.syncedAt != null }.map { it.id }
            if (syncedDeletedIds.isNotEmpty()) {
                journalDao.insertTombstones(syncedDeletedIds.map { DeletedJournalTombstone(it) })
            }
            journalDao.emptyBin()
            journalDao.deleteOrphanedCrossRefs()
            journalDao.deleteOrphanedTags()
        }
    }

    override suspend fun restoreAllJournals() {
        journalDao.restoreAllJournals(System.currentTimeMillis())
    }

    override suspend fun updateJournal(journal: Journal) {
        transactionRunner {
            journalDao.updateJournal(journal.asEntity())
            replaceJournalTags(journal.id, journal.tags)
        }
    }

    /**
     * Rewrites the tag rows for a single journal. Must run inside a transaction: the delete
     * followed by the inserts used to be separate transactions, so a process death in between
     * could leave a journal without any tag cross references.
     *
     * Orphaned tags are intentionally not pruned here. This runs on every autosave, and
     * [JournalDao.deleteOrphanedTags] is a full scan of the cross reference table.
     */
    private suspend fun replaceJournalTags(journalId: String, tags: List<String>) {
        journalDao.deleteTagsForJournal(journalId)
        val normalized = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) return
        journalDao.insertTags(normalized.map { TagEntity(name = it) })
        journalDao.insertJournalTagCrossRefs(normalized.map { JournalTagCrossRef(id = journalId, tagName = it) })
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
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedOld.isEmpty() || trimmedNew.isEmpty() || trimmedOld == trimmedNew) return

        transactionRunner {
            val timestamp = System.currentTimeMillis()
            val affectedJournals = journalDao.getAllJournalsByTagNameSync(trimmedOld)

            journalDao.insertTag(TagEntity(name = trimmedNew))
            journalDao.copyTagCrossRefs(trimmedOld, trimmedNew)
            journalDao.deleteTagCrossRefs(trimmedOld)
            journalDao.deleteTag(trimmedOld)

            val updatedEntities = affectedJournals.mapNotNull { entity ->
                val updatedTags = entity.tags.map { if (it == trimmedOld) trimmedNew else it }.distinct()
                if (updatedTags != entity.tags) {
                    entity.copy(tags = updatedTags, updatedAt = timestamp)
                } else {
                    null
                }
            }

            if (updatedEntities.isNotEmpty()) {
                journalDao.updateJournals(updatedEntities)
            }
        }
    }

    override suspend fun deleteTag(tagName: String) {
        val trimmed = tagName.trim()
        if (trimmed.isEmpty()) return

        transactionRunner {
            val timestamp = System.currentTimeMillis()
            val affectedJournals = journalDao.getAllJournalsByTagNameSync(trimmed)

            journalDao.deleteTagCrossRefs(trimmed)
            journalDao.deleteTag(trimmed)

            val updatedEntities = affectedJournals.mapNotNull { entity ->
                val updatedTags = entity.tags.filter { it != trimmed }
                if (updatedTags != entity.tags) {
                    entity.copy(tags = updatedTags, updatedAt = timestamp)
                } else {
                    null
                }
            }

            if (updatedEntities.isNotEmpty()) {
                journalDao.updateJournals(updatedEntities)
            }
        }
    }

    override suspend fun toggleBookmark(id: String) {
        val journal = journalDao.getJournalById(id) ?: return
        journalDao.updateJournal(journal.copy(
            isBookmarked = !journal.isBookmarked,
            updatedAt = System.currentTimeMillis()
        ))
    }

    override suspend fun toggleLock(id: String) {
        val journal = journalDao.getJournalById(id) ?: return
        val newLocked = !journal.isLocked
        val updated = if (newLocked) {
            journal.copy(
                isLocked = true,
                cloudId = null,
                syncedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            journal.copy(
                isLocked = false,
                updatedAt = System.currentTimeMillis()
            )
        }
        journalDao.updateJournal(updated)
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

    override suspend fun resetAllSyncStatuses() {
        journalDao.resetAllSyncStatuses()
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

    override fun getLockedJournals(): Flow<List<Journal>> {
        return journalDao.getLockedJournals().map { it.asDomain() }
    }

    override fun getLockedJournalsCount(): Flow<Int> {
        return journalDao.getLockedJournalsCount()
    }
}
