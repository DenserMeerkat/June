package com.denser.june.core.data.backup

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.denser.june.core.domain.JournalRepo
import com.denser.june.core.domain.backup.ExportSchema
import com.denser.june.core.domain.backup.RestoreFailedException
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.domain.backup.RestoreResult
import com.denser.june.core.domain.data_classes.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class RestoreImpl(
    private val journalRepo: JournalRepo,
    private val context: Context
) : RestoreRepo {

    companion object {
        private const val TAG = "RestoreImpl"
        private const val MEDIA_FOLDER = "journal_media"
    }

    override suspend fun restoreData(path: String): RestoreResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val mediaDir = File(context.filesDir, MEDIA_FOLDER).apply { if (!exists()) mkdirs() }
                var schema: ExportSchema? = null

                Log.d("RestoreDebug", "Starting restore from: $path")

                context.contentResolver.openInputStream(path.toUri())?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val entryName = entry.name
                            Log.d("RestoreDebug", "Found ZIP entry: $entryName")
                            when {
                                entryName == "journal_data.json" -> {
                                    val jsonString = String(zis.readBytes(), Charsets.UTF_8)

                                    val json = Json { ignoreUnknownKeys = true }
                                    schema = json.decodeFromString<ExportSchema>(jsonString)
                                }
                                entryName.startsWith("media/") && !entry.isDirectory -> {
                                    val fileName = File(entryName).name
                                    val targetFile = File(mediaDir, fileName)
                                    FileOutputStream(targetFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
                if (schema == null) {
                    Log.e("RestoreDebug", "Schema is null after reading zip")
                    return@withContext RestoreResult.Failure(RestoreFailedException.InvalidFile)
                }

                Log.d("RestoreDebug", "Inserting ${schema!!.journals.size} journals into DB")

                schema!!.journals.forEach { journal ->
                    val updatedJournal = remapMediaPaths(journal, mediaDir)
                    val id = journalRepo.insertJournal(updatedJournal)
                    Log.d("RestoreDebug", "Inserted Journal ID: $id") 
                }
                RestoreResult.Success
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid URI", e)
                RestoreResult.Failure(RestoreFailedException.InvalidFile)
            } catch (e: SerializationException) {
                Log.e(TAG, "Schema Mismatch", e)
                RestoreResult.Failure(RestoreFailedException.OldSchema)
            } catch (e: Exception) {
                Log.e("RestoreDebug", "Restore failed exception", e)
                RestoreResult.Failure(RestoreFailedException.InvalidFile)
            }
        }

    private fun remapMediaPaths(journal: Journal, mediaDir: File): Journal {
        if (journal.images.isEmpty()) return journal
        val newPaths = journal.images.map { oldPath ->
            val fileName = File(oldPath).name
            File(mediaDir, fileName).absolutePath
        }
        return journal.copy(images = newPaths)
    }
}