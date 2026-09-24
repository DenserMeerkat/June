package com.denser.june.core.data.database.journal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntity)

    @Update
    suspend fun updateJournal(journal: JournalEntity)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournal(id: String)

    @Query("DELETE FROM journals")
    suspend fun deleteAllJournals()

    @Query("SELECT * FROM journals WHERE deletedAt IS NULL ORDER BY dateTime DESC, createdAt DESC")
    fun getAllJournals(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journals WHERE deletedAt IS NULL ORDER BY dateTime DESC, createdAt DESC")
    suspend fun getAllJournalsSync(): List<JournalEntity>

    @Query("SELECT * FROM journals ORDER BY dateTime DESC, createdAt DESC")
    suspend fun getAllJournalsIncludeDeletedSync(): List<JournalEntity>

    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getJournalById(id: String): JournalEntity?

    @Query("SELECT * FROM journals WHERE deletedAt IS NULL ORDER BY dateTime DESC, createdAt DESC LIMIT 1")
    suspend fun getLatestJournal(): JournalEntity?

    @Query("SELECT * FROM journals WHERE deletedAt IS NULL AND title LIKE '%' || :query || '%' ")
    fun searchJournal(query: String): Flow<List<JournalEntity>>

    @Query(""" SELECT * FROM journals WHERE deletedAt IS NULL AND dateTime >= :startDate AND dateTime <= :endDate ORDER BY dateTime DESC, createdAt DESC """)
    fun getJournalsByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntity>>

    @Query("""
        SELECT * FROM journals 
        WHERE (
            :query IS NULL OR :query = '' OR
            title LIKE '%' || :query || '%' OR 
            content LIKE '%' || :query || '%' OR
            id IN (
                SELECT ref.id 
                FROM journal_tag_cross_ref ref 
                WHERE ref.tagName LIKE '%' || :query || '%'
            ) OR
        (
            (CASE strftime('%m', dateTime / 1000, 'unixepoch')
                WHEN '01' THEN 'January' WHEN '02' THEN 'February' WHEN '03' THEN 'March'
                WHEN '04' THEN 'April'   WHEN '05' THEN 'May'      WHEN '06' THEN 'June'
                WHEN '07' THEN 'July'    WHEN '08' THEN 'August'   WHEN '09' THEN 'September'
                WHEN '10' THEN 'October' WHEN '11' THEN 'November' WHEN '12' THEN 'December'
            END LIKE '%' || REPLACE(:query, ',', '') || '%')
            OR
            (strftime('%Y', dateTime / 1000, 'unixepoch') LIKE '%' || REPLACE(:query, ',', '') || '%')
            OR
            (strftime('%d', dateTime / 1000, 'unixepoch') LIKE '%' || REPLACE(:query, ',', '') || '%')
        ) OR
        
            (location LIKE '%"name":"%' || :query || '%') OR
            (location LIKE '%"address":"%' || :query || '%') OR
            (location LIKE '%"locality":"%' || :query || '%') OR
            (songDetails LIKE '%"title":"%' || :query || '%') OR
            (songDetails LIKE '%"artistName":"%' || :query || '%')
        )
        AND (:isBookmarked IS NULL OR isBookmarked = :isBookmarked)
        AND (:isDraft IS NULL OR isDraft = :isDraft)
        
        AND (:hasMedia IS NULL OR (:hasMedia = 1 AND images != '[]') OR (:hasMedia = 0 AND images = '[]'))
        
        AND (:hasLocation IS NULL OR (:hasLocation = 1 AND location IS NOT NULL) OR (:hasLocation = 0 AND location IS NULL))
        
        AND (:hasSong IS NULL OR (:hasSong = 1 AND songDetails IS NOT NULL) OR (:hasSong = 0 AND songDetails IS NULL))
        AND deletedAt IS NULL
        ORDER BY dateTime DESC, createdAt DESC
    """)
    fun getJournals(
        query: String? = null,
        isBookmarked: Boolean? = null,
        isDraft: Boolean? = null,
        hasLocation: Boolean? = null,
        hasSong: Boolean? = null,
        hasMedia: Boolean? = null
    ): Flow<List<JournalEntity>>

    @Transaction
    @Query("""
        SELECT j.* FROM journals j
        INNER JOIN journal_tag_cross_ref ref ON j.id = ref.id
        WHERE ref.tagName = :tagName AND j.deletedAt IS NULL
        ORDER BY j.dateTime DESC, j.createdAt DESC
    """)
    fun getJournalsByTagName(tagName: String): Flow<List<JournalEntity>>

    @Transaction
    @Query("""
        SELECT j.* FROM journals j
        INNER JOIN journal_tag_cross_ref ref ON j.id = ref.id
        WHERE ref.tagName = :tagName AND j.deletedAt IS NULL
    """)
    suspend fun getJournalsByTagNameSync(tagName: String): List<JournalEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalTagCrossRef(crossRef: JournalTagCrossRef)

    @Query("DELETE FROM journal_tag_cross_ref WHERE id = :journalId")
    suspend fun deleteTagsForJournal(journalId: String)

    @Query("DELETE FROM journal_tag_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM tags WHERE name NOT IN (SELECT DISTINCT tagName FROM journal_tag_cross_ref)")
    suspend fun deleteOrphanedTags()

    @Query("""
        SELECT t.name 
        FROM tags t
        INNER JOIN journal_tag_cross_ref ref ON t.name = ref.tagName
        INNER JOIN journals j ON ref.id = j.id
        WHERE t.name LIKE :query || '%' AND j.deletedAt IS NULL
        GROUP BY t.name
        ORDER BY MAX(j.dateTime) DESC, MAX(j.createdAt) DESC, t.name ASC
    """)
    fun getTagSuggestions(query: String): Flow<List<String>>

    @Query("""
        SELECT t.name 
        FROM tags t
        INNER JOIN journal_tag_cross_ref ref ON t.name = ref.tagName
        INNER JOIN journals j ON ref.id = j.id
        WHERE j.deletedAt IS NULL
        GROUP BY t.name
        ORDER BY MAX(j.dateTime) DESC, MAX(j.createdAt) DESC, t.name ASC
    """)
    fun getAllUniqueTags(): Flow<List<String>>

    @Query("""
        SELECT t.name, COUNT(ref.id) as count
        FROM tags t
        INNER JOIN journal_tag_cross_ref ref ON t.name = ref.tagName
        INNER JOIN journals j ON ref.id = j.id
        WHERE j.deletedAt IS NULL
        GROUP BY t.name
    """)
    fun getAllTagCounts(): Flow<List<TagCount>>

    @Query("""
        SELECT j.* FROM journals j
        INNER JOIN journal_tag_cross_ref ref ON j.id = ref.id
        WHERE ref.tagName IN (:tags) AND j.deletedAt IS NULL
        GROUP BY j.id
        HAVING COUNT(DISTINCT ref.tagName) = :tagCount
        ORDER BY j.dateTime DESC, j.createdAt DESC
    """)
    fun getJournalsWithAllTags(tags: List<String>, tagCount: Int): Flow<List<JournalEntity>>

    @Query("UPDATE tags SET name = :newName WHERE name = :oldName")
    suspend fun updateTagName(oldName: String, newName: String)

    @Query("UPDATE journal_tag_cross_ref SET tagName = :newName WHERE tagName = :oldName")
    suspend fun updateTagCrossRefName(oldName: String, newName: String)

    @Query("DELETE FROM tags WHERE name = :tagName")
    suspend fun deleteTag(tagName: String)

    @Query("SELECT * FROM journals WHERE deletedAt IS NOT NULL ORDER BY updatedAt DESC")
    fun getDeletedJournals(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journals WHERE deletedAt IS NOT NULL ORDER BY updatedAt DESC")
    suspend fun getDeletedJournalsSync(): List<JournalEntity>

    @Query("UPDATE journals SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteJournal(id: String, timestamp: Long)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun hardDeleteJournal(id: String)

    @Query("UPDATE journals SET deletedAt = :timestamp, updatedAt = :timestamp WHERE deletedAt IS NULL")
    suspend fun softDeleteAllJournals(timestamp: Long)

    @Query("UPDATE journals SET deletedAt = NULL, updatedAt = :timestamp WHERE deletedAt IS NOT NULL")
    suspend fun restoreAllJournals(timestamp: Long)

    @Query("DELETE FROM journals WHERE deletedAt IS NOT NULL")
    suspend fun emptyBin()

    @Query("SELECT * FROM journals WHERE (updatedAt > (syncedAt + :threshold) OR syncedAt IS NULL)")
    suspend fun getJournalsToSync(threshold: Long): List<JournalEntity>

    @Query("UPDATE journals SET cloudId = :cloudId, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, cloudId: String, syncedAt: Long)

    @Query("UPDATE journals SET cloudId = NULL, syncedAt = NULL")
    suspend fun resetAllSyncStatuses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: DeletedJournalTombstone)

    @Query("SELECT * FROM deleted_journal_tombstones")
    suspend fun getAllTombstones(): List<DeletedJournalTombstone>

    @Query("DELETE FROM deleted_journal_tombstones WHERE id = :id")
    suspend fun deleteTombstone(id: String)

    @Query("SELECT * FROM journals WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun getOldDeletedJournals(threshold: Long): List<JournalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstones(tombstones: List<DeletedJournalTombstone>)

    @Query("""
        UPDATE journals 
        SET updatedAt = :timestamp 
        WHERE id IN (
            SELECT ref.id FROM journal_tag_cross_ref ref
            WHERE ref.tagName = :tagName
        )
    """)
    suspend fun bumpJournalTimestampsByTag(tagName: String, timestamp: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM journals WHERE (updatedAt > (syncedAt + :threshold) OR syncedAt IS NULL))")
    suspend fun hasUnsyncedJournals(threshold: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM journals WHERE (updatedAt > (syncedAt + :threshold) OR syncedAt IS NULL))")
    fun observeHasUnsyncedJournals(threshold: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_journal_tombstones)")
    suspend fun hasTombstones(): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_journal_tombstones)")
    fun observeHasTombstones(): Flow<Boolean>
}

data class TagCount(
    val name: String,
    val count: Int
)