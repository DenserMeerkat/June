package com.example.june.core.data.backup;

import android.util.Log
import com.example.june.core.domain.NoteRepo;
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.ExportSchema
import com.example.june.core.data.mappers.toNoteSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json

class ExportImpl(
    private val noteRepo:NoteRepo
) : ExportRepo {
    @OptIn(ExperimentalTime::class)
    override suspend fun exportToJson(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val notesData = noteRepo.getAllNotes().map { it.toNoteSchema() }

            Json.Default.encodeToString(
                ExportSchema(
                    schemaVersion = 1,
                    notes = notesData
                )
            )
        } catch (e: Exception) {
            Log.wtf("ExportImpl", e)
            null
        }
    }
}