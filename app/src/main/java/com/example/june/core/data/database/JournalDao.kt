package com.example.june.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals")
    fun getAllJournals(): Flow<List<JournalEntity>>

    @Insert
    suspend fun insertJournal(journal: JournalEntity)

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