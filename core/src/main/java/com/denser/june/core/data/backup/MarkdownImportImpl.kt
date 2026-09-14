package com.denser.june.core.data.backup

import android.content.Context
import android.net.Uri
import com.denser.june.core.domain.backup.MarkdownImportRepo
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.markdown.MarkdownEngine
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class MarkdownImportImpl(
    private val journalRepo: JournalRepository,
    private val context: Context
) : MarkdownImportRepo {

    companion object {
        private const val TAG = "MarkdownImportImpl"
        private const val MEDIA_FOLDER = "journal_media"
    }

    override suspend fun importMarkdownFiles(uris: List<Uri>): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            AppLogger.d(AppLogger.Category.BACKUP, TAG, "Starting markdown file import for ${uris.size} URIs")
            var importedCount = 0

            for (uri in uris) {
                try {
                    val displayName = FileUtils.getDisplayName(context, uri)
                    val contentString = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: continue

                    val journal = MarkdownEngine.fromMarkdown(contentString, displayName)

                    val existing = journalRepo.getJournalById(journal.id)
                    val journalToSave = if (existing != null) {
                        journal.copy(
                            cloudId = existing.cloudId,
                            syncedAt = existing.syncedAt,
                            createdAt = existing.createdAt
                        )
                    } else {
                        journal
                    }

                    journalRepo.insertJournal(journalToSave)
                    importedCount++
                    AppLogger.d(AppLogger.Category.BACKUP, TAG, "Successfully imported markdown journal: ${journalToSave.title} (ID: ${journalToSave.id})")
                } catch (e: Exception) {
                    AppLogger.e(AppLogger.Category.BACKUP, TAG, "Failed to import markdown file from URI: $uri", e)
                }
            }

            if (importedCount > 0) {
                Result.success(importedCount)
            } else {
                Result.failure(Exception("No valid Markdown files could be imported"))
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, TAG, "Import failed with error", e)
            Result.failure(e)
        }
    }

    override suspend fun importMarkdownZip(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            AppLogger.d(AppLogger.Category.BACKUP, TAG, "Starting markdown zip import from $uri")
            val mediaDir = File(context.filesDir, MEDIA_FOLDER).apply { if (!exists()) mkdirs() }
            val parsedJournals = mutableListOf<Journal>()
            val extractedMediaMap = mutableMapOf<String, String>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (!entry.isDirectory) {
                            if (entryName.startsWith("media/")) {
                                val originalFileName = File(entryName).name
                                val extension = originalFileName.substringAfterLast('.', "jpg")
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
                            } else if (entryName.endsWith(".md", ignoreCase = true) || entryName.endsWith(".markdown", ignoreCase = true)) {
                                val mdText = String(zis.readBytes(), Charsets.UTF_8)
                                val fileName = File(entryName).name
                                val journal = MarkdownEngine.fromMarkdown(mdText, fileName)
                                parsedJournals.add(journal)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (parsedJournals.isEmpty()) {
                AppLogger.e(AppLogger.Category.BACKUP, TAG, "No markdown files found in ZIP archive")
                return@withContext Result.failure(Exception("No markdown files found in ZIP archive"))
            }

            AppLogger.d(AppLogger.Category.BACKUP, TAG, "Found ${parsedJournals.size} markdown journals in ZIP. Inserting into DB...")

            var importedCount = 0
            for (journal in parsedJournals) {
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
                journalRepo.insertJournal(journalToSave)
                importedCount++
            }

            AppLogger.d(AppLogger.Category.BACKUP, TAG, "Markdown ZIP import completed. Imported: $importedCount")
            Result.success(importedCount)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, TAG, "Failed to import markdown ZIP", e)
            Result.failure(e)
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
