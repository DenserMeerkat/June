package com.example.june.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<com.example.june.core.data.database.NoteEntity>>

    @Insert
    suspend fun insertNote(note: com.example.june.core.data.database.NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Update
    suspend fun updateNote(note: com.example.june.core.data.database.NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): com.example.june.core.data.database.NoteEntity?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' ")
    fun searchNote(query: String): Flow<List<com.example.june.core.data.database.NoteEntity>>

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}