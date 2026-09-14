package com.denser.june.core.data.backup

import android.content.Context
import com.denser.june.core.data.database.journal.JournalDatabase
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.sync.JournalSyncMeta
import com.denser.june.core.domain.sync.SyncManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ExportImpl(
    private val journalRepo: JournalRepository,
    private val context: Context
) : ExportRepo {

    override suspend fun exportData(includeMedia: Boolean): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Starting export process. Include media: $includeMedia")
            val journals = journalRepo.getAllJournals()

            AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Found ${journals.size} journals to export")

            val mediaDir = File(context.filesDir, "journal_media")
            val cleanedJournals = journals.map { journal ->
                val cleanedImages = journal.images.map { path ->
                    val file = File(path)
                    File(mediaDir, file.name).absolutePath
                }
                journal.copy(images = cleanedImages)
            }

            val journalMeta = cleanedJournals.associate { j ->
                j.id to JournalSyncMeta(rev = 1, contentHash = j.computeContentHash())
            }

            val totalMedia = cleanedJournals.flatMap { it.images }.map { File(it).name }.distinct().size
            val manifest = SyncManifest(
                lastSyncTime = System.currentTimeMillis(),
                lastSyncDeviceId = "backup_export",
                databaseVersion = JournalDatabase.VERSION,
                schemaVersion = SyncManifest.CURRENT_SCHEMA_VERSION,
                totalJournals = cleanedJournals.size,
                totalMedia = totalMedia,
                journalMetadata = journalMeta
            )

            val manifestJson = Json.Default.encodeToString(SyncManifest.serializer(), manifest)
            val backupFile = File(context.cacheDir, "JuneBackup_${System.currentTimeMillis()}.zip")
            val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile)))

            zipOutputStream.use { zos ->
                val manifestEntry = ZipEntry("manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(manifestJson.toByteArray())
                zos.closeEntry()

                cleanedJournals.forEach { journal ->
                    val journalJson = Json.Default.encodeToString(Journal.serializer(), journal)
                    val journalEntry = ZipEntry("journals/${journal.id}.json")
                    zos.putNextEntry(journalEntry)
                    zos.write(journalJson.toByteArray())
                    zos.closeEntry()
                }

                if (includeMedia) {
                    val processedFileNames = mutableSetOf<Pair<String, String>>()
                    var packedMediaCount = 0

                    cleanedJournals.forEach { journal ->
                        journal.images.forEach { absolutePath ->
                            val file = File(absolutePath)
                            if (file.exists() && processedFileNames.add(journal.id to file.name)) {
                                try {
                                    val mediaEntry = ZipEntry("media/${journal.id}/${file.name}")
                                    zos.putNextEntry(mediaEntry)

                                    FileInputStream(file).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                    packedMediaCount++
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack media file: ${file.name}", e)
                                }
                            }
                        }
                    }
                    AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Packed $packedMediaCount media files")
                }
            }

            AppLogger.d(
                AppLogger.Category.BACKUP,
                "ExportImpl",
                "Export completed successfully. Created zip: ${backupFile.name} (size: ${backupFile.length()} bytes)"
            )
            Result.success(backupFile)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Export failed with exception", e)
            Result.failure(e)
        }
    }

    override suspend fun exportAsMarkdown(includeMedia: Boolean): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Starting markdown export. Include media: $includeMedia")
            val journals = journalRepo.getAllJournals()

            AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Found ${journals.size} journals to export as markdown")

            val mediaDir = File(context.filesDir, "journal_media")
            val cleanedJournals = journals.map { journal ->
                val cleanedImages = journal.images.map { path ->
                    val file = File(path)
                    File(mediaDir, file.name).absolutePath
                }
                journal.copy(images = cleanedImages)
            }

            val backupFile = File(context.cacheDir, "JuneMarkdownExport_${System.currentTimeMillis()}.zip")
            val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile)))

            val usedFileNames = mutableSetOf<String>()
            zipOutputStream.use { zos ->
                cleanedJournals.forEach { journal ->
                    val mediaPrefix = if (includeMedia) "media/${journal.id}" else null
                    val markdownText = com.denser.june.core.domain.markdown.MarkdownEngine.toMarkdown(
                        journal = journal,
                        relativeMediaPathPrefix = mediaPrefix,
                        includeMedia = includeMedia
                    )

                    var fileName = com.denser.june.core.domain.markdown.MarkdownEngine.generateFileName(journal)
                    var counter = 1
                    val baseName = fileName.removeSuffix(".md")
                    while (usedFileNames.contains(fileName)) {
                        fileName = "${baseName}_$counter.md"
                        counter++
                    }
                    usedFileNames.add(fileName)

                    val mdEntry = ZipEntry(fileName)
                    zos.putNextEntry(mdEntry)
                    zos.write(markdownText.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    if (includeMedia) {
                        journal.images.forEach { imagePath ->
                            val file = File(imagePath)
                            if (file.exists()) {
                                try {
                                    val mediaEntry = ZipEntry("media/${journal.id}/${file.name}")
                                    zos.putNextEntry(mediaEntry)
                                    FileInputStream(file).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack media file for markdown export: ${file.name}", e)
                                }
                            }
                        }
                    }
                }
            }

            AppLogger.d(
                AppLogger.Category.BACKUP,
                "ExportImpl",
                "Markdown export completed successfully. Created zip: ${backupFile.name} (size: ${backupFile.length()} bytes)"
            )
            Result.success(backupFile)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Markdown export failed with exception", e)
            Result.failure(e)
        }
    }

    override suspend fun exportSingleJournalZip(journal: Journal): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            val fileName = com.denser.june.core.domain.markdown.MarkdownEngine.generateFileName(journal, forSingleExport = true)
            val baseName = fileName.removeSuffix(".md")
            val zipFile = File(context.cacheDir, "${baseName}_${System.currentTimeMillis()}.zip")

            val markdownText = com.denser.june.core.domain.markdown.MarkdownEngine.toMarkdown(
                journal = journal,
                relativeMediaPathPrefix = "media",
                includeMedia = true
            )

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val mdEntry = ZipEntry(fileName)
                zos.putNextEntry(mdEntry)
                zos.write(markdownText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                val processedFileNames = mutableSetOf<String>()
                journal.images.forEach { imagePath ->
                    val file = File(imagePath)
                    if (file.exists() && processedFileNames.add(file.name)) {
                        try {
                            val mediaEntry = ZipEntry("media/${file.name}")
                            zos.putNextEntry(mediaEntry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack media file for single export: ${file.name}", e)
                        }
                    }
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to export single journal zip", e)
            Result.failure(e)
        }
    }
}