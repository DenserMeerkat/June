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

    @Query("SELECT * FROM journals WHERE isDeleted = 0 ORDER BY dateTime DESC")
    fun getAllJournals(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journals WHERE isDeleted = 0 ORDER BY dateTime DESC")
    suspend fun getAllJournalsSync(): List<JournalEntity>

    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getJournalById(id: String): JournalEntity?

    @Query("SELECT * FROM journals WHERE isDeleted = 0 ORDER BY dateTime DESC LIMIT 1")
    suspend fun getLatestJournal(): JournalEntity?

    @Query("SELECT * FROM journals WHERE isDeleted = 0 AND title LIKE '%' || :query || '%' ")
    fun searchJournal(query: String): Flow<List<JournalEntity>>

    @Query(""" SELECT * FROM journals WHERE isDeleted = 0 AND dateTime >= :startDate AND dateTime <= :endDate ORDER BY dateTime DESC """)
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
                INNER JOIN tags t ON ref.tagId = t.tagId 
                WHERE t.name LIKE '%' || :query || '%'
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
        
        AND (:hasMedia IS NULL OR (:hasMedia = 1 AND images IS NOT NULL) OR (:hasMedia = 0 AND images IS NULL))
        
        AND (:hasLocation IS NULL OR (:hasLocation = 1 AND location IS NOT NULL) OR (:hasLocation = 0 AND location IS NULL))
        
        AND (:hasSong IS NULL OR (:hasSong = 1 AND songDetails IS NOT NULL) OR (:hasSong = 0 AND songDetails IS NULL))
        AND isDeleted = 0
        ORDER BY dateTime DESC
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
        INNER JOIN tags t ON ref.tagId = t.tagId
        WHERE t.name = :tagName AND j.isDeleted = 0
        ORDER BY j.dateTime DESC
    """)
    fun getJournalsByTagName(tagName: String): Flow<List<JournalEntity>>

    @Transaction
    @Query("""
        SELECT j.* FROM journals j
        INNER JOIN journal_tag_cross_ref ref ON j.id = ref.id
        INNER JOIN tags t ON ref.tagId = t.tagId
        WHERE t.name = :tagName AND j.isDeleted = 0
    """)
    suspend fun getJournalsByTagNameSync(tagName: String): List<JournalEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT tagId FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagIdByName(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalTagCrossRef(crossRef: JournalTagCrossRef)

    @Query("DELETE FROM journal_tag_cross_ref WHERE id = :journalId")
    suspend fun deleteTagsForJournal(journalId: String)

    @Query("DELETE FROM journal_tag_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM tags WHERE tagId NOT IN (SELECT DISTINCT tagId FROM journal_tag_cross_ref)")
    suspend fun deleteOrphanedTags()

    @Query("""
        SELECT t.name 
        FROM tags t
        LEFT JOIN journal_tag_cross_ref ref ON t.tagId = ref.tagId
        LEFT JOIN journals j ON ref.id = j.id
        WHERE t.name LIKE :query || '%'
        GROUP BY t.tagId
        ORDER BY MAX(j.dateTime) DESC, t.name ASC
    """)
    fun getTagSuggestions(query: String): Flow<List<String>>

    @Query("""
        SELECT t.name 
        FROM tags t
        LEFT JOIN journal_tag_cross_ref ref ON t.tagId = ref.tagId
        LEFT JOIN journals j ON ref.id = j.id
        GROUP BY t.tagId
        ORDER BY MAX(j.dateTime) DESC, t.name ASC
    """)
    fun getAllUniqueTags(): Flow<List<String>>

    @Query("""
        SELECT t.name, COUNT(ref.id) as count
        FROM tags t
        INNER JOIN journal_tag_cross_ref ref ON t.tagId = ref.tagId
        GROUP BY t.name
    """)
    fun getAllTagCounts(): Flow<List<TagCount>>

    @Query("""
        SELECT j.* FROM journals j
        INNER JOIN journal_tag_cross_ref ref ON j.id = ref.id
        INNER JOIN tags t ON ref.tagId = t.tagId
        WHERE t.name IN (:tags) AND j.isDeleted = 0
        GROUP BY j.id
        HAVING COUNT(DISTINCT t.name) = :tagCount
        ORDER BY j.dateTime DESC
    """)
    fun getJournalsWithAllTags(tags: List<String>, tagCount: Int): Flow<List<JournalEntity>>

    @Query("UPDATE tags SET name = :newName WHERE name = :oldName")
    suspend fun updateTagName(oldName: String, newName: String)

    @Query("DELETE FROM tags WHERE name = :tagName")
    suspend fun deleteTag(tagName: String)

    @Query("SELECT * FROM journals WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedJournals(): Flow<List<JournalEntity>>

    @Query("UPDATE journals SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteJournal(id: String, timestamp: Long)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun hardDeleteJournal(id: String)

    @Query("UPDATE journals SET isDeleted = 1, updatedAt = :timestamp WHERE isDeleted = 0")
    suspend fun softDeleteAllJournals(timestamp: Long)

    @Query("DELETE FROM journals WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM journals WHERE updatedAt > :lastSyncTime")
    suspend fun getJournalsToSync(lastSyncTime: Long): List<JournalEntity>

    @Query("UPDATE journals SET cloudId = :cloudId, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, cloudId: String, syncedAt: Long)
}

data class TagCount(
    val name: String,
    val count: Int
)