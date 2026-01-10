package com.denser.june.core.data.database.journal

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

    @Query("""
        SELECT * FROM journals 
        WHERE dateTime >= :startDate AND dateTime <= :endDate
        ORDER BY dateTime DESC
    """)
    fun getJournalsByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntity>>

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