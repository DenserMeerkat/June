package com.denser.june.core.sync.fakes

import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * In-memory FakeJournalRepository for unit testing.
 */
class FakeJournalRepository : JournalRepository {

    val db = mutableMapOf<String, Journal>()
    val tombstones = mutableSetOf<String>()

    private val _journalFlow = MutableStateFlow<List<Journal>>(emptyList())

    fun seed(journal: Journal) {
        db[journal.id] = journal
        _journalFlow.value = db.values.filter { it.deletedAt == null }.toList()
    }

    fun seedSynced(journal: Journal, syncedAt: Long) = seed(journal.copy(syncedAt = syncedAt, cloudId = "${journal.id}.json"))

    override fun getJournals(query: String?, isBookmarked: Boolean?, isDraft: Boolean?, hasLocation: Boolean?, hasSong: Boolean?, hasMedia: Boolean?): Flow<List<Journal>> =
        _journalFlow

    override suspend fun getAllJournals(): List<Journal> =
        db.values.filter { it.deletedAt == null }

    override suspend fun getJournalById(id: String): Journal? = db[id]

    override suspend fun getLatestJournal(): Journal? =
        db.values.filter { it.deletedAt == null }.maxByOrNull { it.dateTime }

    override fun getJournalsByDateRange(startDate: Long, endDate: Long): Flow<List<Journal>> =
        _journalFlow.map { it.filter { j -> j.dateTime in startDate..endDate } }

    override fun getJournalsByMultipleTags(tags: List<String>): Flow<List<Journal>> =
        _journalFlow.map { it.filter { j -> j.tags.containsAll(tags) } }

    override suspend fun insertJournal(journal: Journal): String {
        db[journal.id] = journal
        _journalFlow.value = db.values.filter { it.deletedAt == null }.toList()
        return journal.id
    }

    override suspend fun updateJournal(journal: Journal) { insertJournal(journal) }

    override suspend fun softDeleteJournal(id: String) {
        db[id]?.let {
            db[id] = it.copy(deletedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        }
        _journalFlow.value = db.values.filter { it.deletedAt == null }.toList()
    }

    override suspend fun restoreJournal(id: String) {
        db[id]?.let { db[id] = it.copy(deletedAt = null) }
        _journalFlow.value = db.values.filter { it.deletedAt == null }.toList()
    }

    override suspend fun hardDeleteJournal(id: String) {
        db.remove(id)
        tombstones.remove(id)
        _journalFlow.value = db.values.filter { it.deletedAt == null }.toList()
    }

    override suspend fun deleteAllJournals() { db.clear(); _journalFlow.value = emptyList() }

    override suspend fun emptyBin() {
        val deletedIds = journalDaoGetDeletedJournalsSync()
        if (deletedIds.isNotEmpty()) {
            db.entries.removeIf { it.value.deletedAt != null }
        }
    }

    private fun journalDaoGetDeletedJournalsSync() = db.values.filter { it.deletedAt != null }

    override suspend fun restoreAllJournals() {
        db.keys.forEach { id -> db[id] = db[id]!!.copy(deletedAt = null) }
        _journalFlow.value = db.values.toList()
    }

    override fun getDeletedJournals(): Flow<List<Journal>> =
        _journalFlow.map { _ -> db.values.filter { it.deletedAt != null } }

    override suspend fun getJournalsToSync(threshold: Long): List<Journal> =
        db.values.filter { j ->
            j.deletedAt == null && ((j.updatedAt ?: 0L) > ((j.syncedAt ?: 0L) + threshold) || j.syncedAt == null)
        }

    override suspend fun getAllJournalsIncludeDeletedSync(): List<Journal> = db.values.toList()

    override suspend fun getOldDeletedJournals(threshold: Long): List<Journal> =
        db.values.filter { it.deletedAt != null && it.deletedAt!! < threshold }

    override suspend fun updateSyncStatus(id: String, cloudId: String, syncedAt: Long) {
        db[id]?.let { db[id] = it.copy(cloudId = cloudId, syncedAt = syncedAt) }
    }

    override suspend fun resetAllSyncStatuses() {
        db.keys.forEach { id -> db[id] = db[id]!!.copy(cloudId = null, syncedAt = null) }
    }

    override suspend fun getAllTombstones(): List<String> = tombstones.toList()

    override suspend fun deleteTombstone(id: String) { tombstones.remove(id) }

    override fun getTagSuggestions(query: String): Flow<List<String>> = flowOf(emptyList())
    override fun getUniqueTags(): Flow<List<String>> = flowOf(emptyList())
    override fun getTagCounts(): Flow<Map<String, Int>> = flowOf(emptyMap())
    override suspend fun toggleBookmark(id: String) { db[id]?.let { db[id] = it.copy(isBookmarked = !it.isBookmarked) } }
    override suspend fun renameTag(oldName: String, newName: String) {}
    override suspend fun deleteTag(tagName: String) {}
    override fun observeHasUnsyncedJournals(threshold: Long): Flow<Boolean> =
        _journalFlow.map { _ -> hasTombstones() || hasUnsyncedJournals(threshold) }
    override suspend fun hasUnsyncedJournals(threshold: Long): Boolean =
        db.values.any { j -> j.deletedAt == null && ((j.updatedAt ?: 0L) > ((j.syncedAt ?: 0L) + threshold) || j.syncedAt == null) }
    override fun observeHasTombstones(): Flow<Boolean> = flowOf(tombstones.isNotEmpty())
    override suspend fun hasTombstones(): Boolean = tombstones.isNotEmpty()
}
