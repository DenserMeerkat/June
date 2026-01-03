package com.example.june.core.data.backup;

import android.util.Log
import com.example.june.core.domain.JournalRepo;
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.ExportSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json

class ExportImpl(
    private val journalRepo: JournalRepo
) : ExportRepo {
    @OptIn(ExperimentalTime::class)
    override suspend fun exportToJson(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val journalsData = journalRepo.getAllJournals()

            Json.Default.encodeToString(
                ExportSchema(
                    schemaVersion = 1,
                    journals = journalsData
                )
            )
        } catch (e: Exception) {
            Log.wtf("ExportImpl", e)
            null
        }
    }
}