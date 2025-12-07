package com.example.june.core.data.repository

import com.example.june.core.data.database.NoteDao
import com.example.june.core.data.mappers.toEntity
import com.example.june.core.data.mappers.toNote
import com.example.june.core.domain.data_classes.Note
import com.example.june.core.domain.NoteRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NoteRepository(
    private val localDao: NoteDao
) : NoteRepo {

    override suspend fun insertNote(note: Note) {
        withContext(Dispatchers.IO) {
            localDao.insertNote(note.toEntity())
        }
    }

    override fun getNotes(): Flow<List<Note>> {
        return localDao.getAllNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }

    override suspend fun getAllNotes(): List<Note> {
        return localDao.getAllNotes().first().map { it.toNote() }
    }

    override suspend fun getNoteById(id: Long): Note? {
        return withContext(Dispatchers.IO) {
            localDao.getNoteById(id)?.toNote()
        }
    }

    override suspend fun searchNotes(query: String): Flow<List<Note>> {
        return localDao.searchNote(query).map { entities ->
            entities.map { it.toNote() }
        }
    }

    override suspend fun updateNote(note: Note) {
        withContext(Dispatchers.IO) {
            localDao.updateNote(note.toEntity())
        }
    }

    override suspend fun deleteNote(id: Long) {
        withContext(Dispatchers.IO) {
            localDao.deleteNote(id)
        }
    }

    override suspend fun deleteAllNotes() {
        withContext(Dispatchers.IO) {
            localDao.deleteAllNotes()
        }
    }
}
