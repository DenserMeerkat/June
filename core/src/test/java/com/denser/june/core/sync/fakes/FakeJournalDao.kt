package com.denser.june.core.sync.fakes

import com.denser.june.core.data.database.journal.DeletedJournalTombstone
import com.denser.june.core.data.database.journal.JournalDao
import com.denser.june.core.data.database.journal.JournalEntity
import com.denser.june.core.data.database.journal.JournalTagCrossRef
import com.denser.june.core.data.database.journal.TagCount
import com.denser.june.core.data.database.journal.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeJournalDao : JournalDao {
    val journals = mutableMapOf<String, JournalEntity>()
    val tags = mutableSetOf<String>()
    val crossRefs = mutableSetOf<JournalTagCrossRef>()
    val tombstones = mutableListOf<DeletedJournalTombstone>()

    override suspend fun insertJournal(journal: JournalEntity) {
        journals[journal.id] = journal
    }

    override suspend fun updateJournal(journal: JournalEntity) {
        journals[journal.id] = journal
    }

    override suspend fun updateJournals(journals: List<JournalEntity>) {
        journals.forEach { this.journals[it.id] = it }
    }

    override suspend fun deleteJournal(id: String) {
        journals.remove(id)
    }

    override suspend fun deleteAllJournals() {
        journals.clear()
    }

    override fun getAllJournals(): Flow<List<JournalEntity>> =
        flowOf(journals.values.filter { it.deletedAt == null && !it.isLocked })

    override suspend fun getAllJournalsSync(): List<JournalEntity> =
        journals.values.filter { it.deletedAt == null && !it.isLocked }

    override suspend fun getAllJournalsIncludeDeletedSync(): List<JournalEntity> =
        journals.values.toList()

    override suspend fun getJournalById(id: String): JournalEntity? = journals[id]

    override suspend fun getLatestJournal(): JournalEntity? =
        journals.values.maxByOrNull { it.dateTime }

    override fun searchJournal(query: String): Flow<List<JournalEntity>> = flowOf(emptyList())

    override fun getJournalsByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntity>> =
        flowOf(emptyList())

    override fun getJournals(
        query: String?,
        isBookmarked: Boolean?,
        isDraft: Boolean?,
        hasLocation: Boolean?,
        hasSong: Boolean?,
        hasMedia: Boolean?
    ): Flow<List<JournalEntity>> = flowOf(emptyList())

    override fun getJournalsByTagName(tagName: String): Flow<List<JournalEntity>> {
        val matchingIds = crossRefs.filter { it.tagName == tagName }.map { it.id }.toSet()
        return flowOf(journals.values.filter { it.id in matchingIds && it.deletedAt == null && !it.isLocked })
    }

    override suspend fun getJournalsByTagNameSync(tagName: String): List<JournalEntity> {
        val matchingIds = crossRefs.filter { it.tagName == tagName }.map { it.id }.toSet()
        return journals.values.filter { it.id in matchingIds && it.deletedAt == null && !it.isLocked }
    }

    override suspend fun insertTags(tags: List<TagEntity>) {
        tags.forEach { this.tags.add(it.name) }
    }

    override suspend fun insertTag(tag: TagEntity) {
        tags.add(tag.name)
    }

    override suspend fun insertJournalTagCrossRefs(crossRefs: List<JournalTagCrossRef>) {
        this.crossRefs.addAll(crossRefs)
    }

    override suspend fun deleteTagsForJournal(journalId: String) {
        crossRefs.removeAll { it.id == journalId }
    }

    override suspend fun deleteOrphanedCrossRefs() {
        val validIds = journals.keys
        crossRefs.removeAll { it.id !in validIds }
    }

    override suspend fun deleteAllCrossRefs() {
        crossRefs.clear()
    }

    override suspend fun deleteAllTags() {
        tags.clear()
    }

    override suspend fun deleteOrphanedTags() {
        val usedTags = crossRefs.map { it.tagName }.toSet()
        tags.removeAll { it !in usedTags }
    }

    override fun getTagSuggestions(query: String): Flow<List<String>> = flowOf(emptyList())

    override fun getAllUniqueTags(): Flow<List<String>> = flowOf(tags.toList())

    override fun getAllTagCounts(): Flow<List<TagCount>> = flowOf(emptyList())

    override fun getJournalsWithAllTags(tags: List<String>, tagCount: Int): Flow<List<JournalEntity>> =
        flowOf(emptyList())

    override fun getLockedJournals(): Flow<List<JournalEntity>> =
        flowOf(journals.values.filter { it.deletedAt == null && it.isLocked })

    override fun getLockedJournalsCount(): Flow<Int> =
        flowOf(journals.values.count { it.deletedAt == null && it.isLocked })

    override suspend fun updateTagName(oldName: String, newName: String) {
        if (tags.remove(oldName)) {
            tags.add(newName)
        }
    }

    override suspend fun getAllJournalsByTagNameSync(tagName: String): List<JournalEntity> {
        val matchingIds = crossRefs.filter { it.tagName == tagName }.map { it.id }.toSet()
        return journals.values.filter { it.id in matchingIds }
    }

    override suspend fun copyTagCrossRefs(oldName: String, newName: String) {
        val newRefs = crossRefs.filter { it.tagName == oldName }.map { JournalTagCrossRef(it.id, newName) }
        crossRefs.addAll(newRefs)
    }

    override suspend fun deleteTagCrossRefs(tagName: String) {
        crossRefs.removeAll { it.tagName == tagName }
    }

    override suspend fun updateTagCrossRefName(oldName: String, newName: String) {
        copyTagCrossRefs(oldName, newName)
        deleteTagCrossRefs(oldName)
    }

    override suspend fun deleteTag(tagName: String) {
        tags.remove(tagName)
    }

    override fun getDeletedJournals(): Flow<List<JournalEntity>> =
        flowOf(journals.values.filter { it.deletedAt != null })

    override suspend fun getDeletedJournalsSync(): List<JournalEntity> =
        journals.values.filter { it.deletedAt != null }

    override suspend fun softDeleteJournal(id: String, timestamp: Long) {
        journals[id]?.let { journals[id] = it.copy(deletedAt = timestamp, updatedAt = timestamp) }
    }

    override suspend fun hardDeleteJournal(id: String) {
        journals.remove(id)
    }

    override suspend fun softDeleteAllJournals(timestamp: Long) {
        journals.keys.forEach { id ->
            journals[id]?.let { journals[id] = it.copy(deletedAt = timestamp, updatedAt = timestamp) }
        }
    }

    override suspend fun restoreAllJournals(timestamp: Long) {
        journals.keys.forEach { id ->
            journals[id]?.let { journals[id] = it.copy(deletedAt = null, updatedAt = timestamp) }
        }
    }

    override suspend fun emptyBin() {
        journals.values.removeAll { it.deletedAt != null }
    }

    override suspend fun getJournalsToSync(threshold: Long): List<JournalEntity> = emptyList()

    override suspend fun updateSyncStatus(id: String, cloudId: String, syncedAt: Long) {
        journals[id]?.let { journals[id] = it.copy(cloudId = cloudId, syncedAt = syncedAt) }
    }

    override suspend fun resetAllSyncStatuses() {
        journals.keys.forEach { id ->
            journals[id]?.let { journals[id] = it.copy(cloudId = null, syncedAt = null) }
        }
    }

    override suspend fun insertTombstone(tombstone: DeletedJournalTombstone) {
        tombstones.add(tombstone)
    }

    override suspend fun getAllTombstones(): List<DeletedJournalTombstone> = tombstones.toList()

    override suspend fun deleteTombstone(id: String) {
        tombstones.removeAll { it.id == id }
    }

    override suspend fun getOldDeletedJournals(threshold: Long): List<JournalEntity> = emptyList()

    override suspend fun insertTombstones(tombstones: List<DeletedJournalTombstone>) {
        this.tombstones.addAll(tombstones)
    }

    override suspend fun bumpJournalTimestampsByTag(tagName: String, timestamp: Long) {
        val ids = crossRefs.filter { it.tagName == tagName }.map { it.id }.toSet()
        ids.forEach { id ->
            journals[id]?.let { journals[id] = it.copy(updatedAt = timestamp) }
        }
    }

    override suspend fun hasUnsyncedJournals(threshold: Long): Boolean = false

    override fun observeHasUnsyncedJournals(threshold: Long): Flow<Boolean> = flowOf(false)

    override suspend fun hasTombstones(): Boolean = tombstones.isNotEmpty()

    override fun observeHasTombstones(): Flow<Boolean> = flowOf(tombstones.isNotEmpty())
}
