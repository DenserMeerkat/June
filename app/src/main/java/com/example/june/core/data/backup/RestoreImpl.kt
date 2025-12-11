package com.example.june.core.data.backup

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.example.june.core.data.mappers.toJournal
import com.example.june.core.domain.JournalRepo
import com.example.june.core.domain.backup.ExportSchema
import com.example.june.core.domain.backup.RestoreFailedException
import com.example.june.core.domain.backup.RestoreRepo
import com.example.june.core.domain.backup.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.readText


class RestoreImpl(
    private val journalRepo: JournalRepo,
    private val context: Context
) : RestoreRepo {

    companion object {
        private const val TAG = "RestoreImpl"
    }

    override suspend fun restoreJournals(path: String): RestoreResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = createTempFile()

            try {
                context.contentResolver.openInputStream(path.toUri()).use { input ->
                    file.outputStream().use { output ->
                        input?.copyTo(output)
                    }
                }

                val json = Json {
                    ignoreUnknownKeys = true
                }

                val jsonDeserialized = json.decodeFromString<ExportSchema>(file.readText())

                jsonDeserialized.journals
                    .map { it.toJournal() }
                    .forEach { journalRepo.insertJournal(it) }
            } finally {
                file.deleteIfExists()
            }

            RestoreResult.Success
        } catch (e: IllegalArgumentException) {
            Log.wtf(TAG, e)

            RestoreResult.Failure(RestoreFailedException.InvalidFile)
        } catch (e: SerializationException) {
            Log.wtf(TAG, e)

            RestoreResult.Failure(RestoreFailedException.OldSchema)
        }
    }
}