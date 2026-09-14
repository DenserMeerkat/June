package com.denser.june.core.data.backup

import android.content.Context
import androidx.core.net.toUri
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.backup.ExportSchema
import com.denser.june.core.domain.backup.RestoreException
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class RestoreImpl(
    private val journalRepo: JournalRepository,
    private val context: Context
) : RestoreRepo {

    companion object {
        private const val TAG = "RestoreImpl"
        private const val MEDIA_FOLDER = "journal_media"
    }

    override suspend fun restoreData(path: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val mediaDir = File(context.filesDir, MEDIA_FOLDER).apply { if (!exists()) mkdirs() }
                val journalsList = mutableListOf<Journal>()
                var isLegacy = false
                var isMarkdown = false

                AppLogger.d(AppLogger.Category.BACKUP, TAG, "Starting restore from provided backup path")

                context.contentResolver.openInputStream(path.toUri())?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "journal_data.json") {
                                isLegacy = true
                            } else if (entry.name.endsWith(".md", ignoreCase = true) || entry.name.endsWith(".markdown", ignoreCase = true)) {
                                isMarkdown = true
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }

                AppLogger.d(AppLogger.Category.BACKUP, TAG, "Backup type detected - isLegacy: $isLegacy, isMarkdown: $isMarkdown")

                val extractedMediaMap = mutableMapOf<String, String>()

                context.contentResolver.openInputStream(path.toUri())?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val entryName = entry.name
                            when {
                                isLegacy -> {
                                    if (entryName == "journal_data.json") {
                                        val jsonString = String(zis.readBytes(), Charsets.UTF_8)
                                        val json = Json { ignoreUnknownKeys = true }
                                        val schema = json.decodeFromString<ExportSchema>(jsonString)
                                        journalsList.addAll(schema.journals)
                                    } else if (entryName.startsWith("media/") && !entry.isDirectory) {
                                        val fileName = File(entryName).name
                                        val targetFile = File(mediaDir, fileName)
                                        FileOutputStream(targetFile).use { fos ->
                                            zis.copyTo(fos)
                                        }
                                        extractedMediaMap[entryName] = targetFile.absolutePath
                                        extractedMediaMap[fileName] = targetFile.absolutePath
                                    }
                                }
                                isMarkdown -> {
                                    if ((entryName.startsWith("journals/") || !entryName.contains("/")) &&
                                        (entryName.endsWith(".md", ignoreCase = true) || entryName.endsWith(".markdown", ignoreCase = true))
                                    ) {
                                        val mdText = String(zis.readBytes(), Charsets.UTF_8)
                                        val fileName = File(entryName).name
                                        val journal = com.denser.june.core.domain.markdown.MarkdownEngine.fromMarkdown(mdText, fileName)
                                        journalsList.add(journal)
                                    } else if (entryName.startsWith("media/") && !entry.isDirectory) {
                                        val originalFileName = File(entryName).name
                                        val safeFileName = "media_${System.currentTimeMillis()}_${(0..9999).random()}_$originalFileName"
                                        val targetFile = File(mediaDir, safeFileName)
                                        FileOutputStream(targetFile).use { fos ->
                                            zis.copyTo(fos)
                                        }
                                        val absPath = targetFile.absolutePath
                                        extractedMediaMap[entryName] = absPath
                                        extractedMediaMap[entryName.removePrefix("media/").trimStart('/')] = absPath
                                        extractedMediaMap["../$entryName"] = absPath
                                        extractedMediaMap[originalFileName] = absPath
                                    }
                                }
                                else -> {
                                    if (entryName.startsWith("journals/") && entryName.endsWith(".json")) {
                                        val jsonString = String(zis.readBytes(), Charsets.UTF_8)
                                        val json = Json { ignoreUnknownKeys = true }
                                        val journal = json.decodeFromString<Journal>(jsonString)
                                        journalsList.add(journal)
                                    } else if (entryName.startsWith("media/") && !entry.isDirectory) {
                                        val fileName = File(entryName).name
                                        val targetFile = File(mediaDir, fileName)
                                        FileOutputStream(targetFile).use { fos ->
                                            zis.copyTo(fos)
                                        }
                                        extractedMediaMap[entryName] = targetFile.absolutePath
                                        extractedMediaMap[fileName] = targetFile.absolutePath
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }

                if (journalsList.isEmpty()) {
                    AppLogger.e(AppLogger.Category.BACKUP, TAG, "No journals found in backup file to restore")
                    return@withContext Result.failure(RestoreException.InvalidFile)
                }

                AppLogger.d(AppLogger.Category.BACKUP, TAG, "Found ${journalsList.size} journals to import. Inserting into DB...")

                journalsList.forEach { journal ->
                    val updatedJournal = remapMediaPaths(journal, extractedMediaMap, mediaDir)
                    val existing = journalRepo.getJournalById(updatedJournal.id)
                    val journalToSave = if (existing != null) {
                        updatedJournal.copy(
                            cloudId = existing.cloudId,
                            syncedAt = existing.syncedAt,
                            createdAt = existing.createdAt
                        )
                    } else {
                        updatedJournal
                    }
                    val id = journalRepo.insertJournal(journalToSave)
                    AppLogger.d(AppLogger.Category.BACKUP, TAG, "Successfully imported journal with ID: $id")
                }
                
                AppLogger.d(AppLogger.Category.BACKUP, TAG, "Restore completed successfully.")
                Result.success(Unit)
            } catch (e: IllegalArgumentException) {
                AppLogger.e(AppLogger.Category.BACKUP, TAG, "Restore failed: Invalid URI", e)
                Result.failure(RestoreException.InvalidFile)
            } catch (e: SerializationException) {
                AppLogger.e(AppLogger.Category.BACKUP, TAG, "Restore failed: Schema Mismatch or Malformed JSON.", e)
                Result.failure(RestoreException.OldSchema)
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Category.BACKUP, TAG, "Restore failed: Unexpected error during ZIP extraction or DB insertion", e)
                Result.failure(RestoreException.InvalidFile)
            }
        }

    private fun remapMediaPaths(journal: Journal, extractedMediaMap: Map<String, String>, mediaDir: File): Journal {
        if (journal.images.isEmpty()) return journal
        val newPaths = journal.images.map { oldPath ->
            val clean = oldPath.trim()
            val fileName = File(clean).name
            val mapped = extractedMediaMap[clean]
                ?: extractedMediaMap[clean.removePrefix("../")]
                ?: extractedMediaMap[clean.removePrefix("media/")]
                ?: extractedMediaMap["${journal.id}/$fileName"]
                ?: extractedMediaMap[fileName]

            if (mapped != null && File(mapped).exists()) {
                mapped
            } else {
                val direct = File(mediaDir, fileName)
                if (direct.exists()) direct.absolutePath else oldPath
            }
        }
        return journal.copy(images = newPaths)
    }
}