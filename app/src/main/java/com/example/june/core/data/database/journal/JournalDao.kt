package com.example.june.core.data.database.journal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("""
        SELECT * FROM journals 
        WHERE (
            :query IS NULL OR :query = '' OR
            title LIKE '%' || :query || '%' OR 
            content LIKE '%' || :query || '%' OR
            
            json_extract(location, '$.name') LIKE '%' || :query || '%' OR
            json_extract(location, '$.address') LIKE '%' || :query || '%' OR
            json_extract(location, '$.locality') LIKE '%' || :query || '%' OR 
            
            json_extract(songDetails, '$.title') LIKE '%' || :query || '%' OR 
            json_extract(songDetails, '$.artistName') LIKE '%' || :query || '%'
        )
        AND (:isBookmarked IS NULL OR isBookmarked = :isBookmarked)
        AND (:isDraft IS NULL OR isDraft = :isDraft)
        
        AND (:hasLocation IS NULL OR (:hasLocation = 1 AND location IS NOT NULL) OR (:hasLocation = 0 AND location IS NULL))
        
        AND (:hasSong IS NULL OR (:hasSong = 1 AND songDetails IS NOT NULL) OR (:hasSong = 0 AND songDetails IS NULL))
        
        ORDER BY dateTime DESC
    """)
    fun getJournals(
        query: String? = null,
        isBookmarked: Boolean? = null,
        isDraft: Boolean? = null,
        hasLocation: Boolean? = null,
        hasSong: Boolean? = null
    ): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journals")
    fun getAllJournals(): Flow<List<JournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalEntity): Long

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournal(id: Long)

    @Update
    suspend fun updateJournal(journal: JournalEntity)

    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getJournalById(id: Long): JournalEntity?

    @Query("SELECT * FROM journals WHERE title LIKE '%' || :query || '%' ")
    fun searchJournal(query: String): Flow<List<JournalEntity>>

    @Query("DELETE FROM journals")
    suspend fun deleteAllJournals()
}