package com.example.june.core.domain

import com.example.june.core.domain.data_classes.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepo {
    suspend fun insertNote(note: Note)
    fun getNotes(): Flow<List<Note>>
    suspend fun getAllNotes(): List<Note>
    suspend fun getNoteById(id: Long): Note?
    suspend fun searchNotes(query: String): Flow<List<Note>>
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(id: Long)
    suspend fun deleteAllNotes()
}