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

    override suspend fun exportData(includeMedia: Boolean): File? = withContext(Dispatchers.IO) {
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
            backupFile
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Export failed with exception", e)
            null
        }
    }

    override suspend fun exportAsMarkdown(includeMedia: Boolean): File? = withContext(Dispatchers.IO) {
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

            zipOutputStream.use { zos ->
                val yamlDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())

                cleanedJournals.forEach { journal ->
                    val sb = StringBuilder()
                    sb.append("---\n")
                    sb.append("id: ${journal.id}\n")
                    sb.append("title: \"${journal.title.replace("\"", "\\\"")}\"\n")
                    journal.emoji?.let { sb.append("emoji: \"${it.replace("\"", "\\\"")}\"\n") }

                    val readableCreatedAt = yamlDateFormatter.format(Instant.ofEpochMilli(journal.createdAt))
                    val readableUpdatedAt = journal.updatedAt?.let { yamlDateFormatter.format(Instant.ofEpochMilli(it)) }
                    val readableDateTime = yamlDateFormatter.format(Instant.ofEpochMilli(journal.dateTime))

                    sb.append("createdAt: \"$readableCreatedAt\"\n")
                    readableUpdatedAt?.let { sb.append("updatedAt: \"$it\"\n") }
                    sb.append("dateTime: \"$readableDateTime\"\n")
                    sb.append("isBookmarked: ${journal.isBookmarked}\n")
                    sb.append("isArchived: ${journal.isArchived}\n")
                    if (journal.tags.isNotEmpty()) {
                        sb.append("tags:\n")
                        journal.tags.forEach { tag ->
                            sb.append("  - \"${tag.replace("\"", "\\\"")}\"\n")
                        }
                    }
                    journal.location?.let { loc ->
                        sb.append("location:\n")
                        sb.append("  latitude: ${loc.latitude}\n")
                        sb.append("  longitude: ${loc.longitude}\n")
                        loc.address?.let { sb.append("  address: \"${it.replace("\"", "\\\"")}\"\n") }
                        loc.name?.let { sb.append("  name: \"${it.replace("\"", "\\\"")}\"\n") }
                        loc.locality?.let { sb.append("  locality: \"${it.replace("\"", "\\\"")}\"\n") }
                    }
                    val song = journal.songDetails
                    if (song != null) {
                        sb.append("song:\n")
                        sb.append("  title: \"${song.title.replace("\"", "\\\"")}\"\n")
                        sb.append("  artistName: \"${song.artistName.replace("\"", "\\\"")}\"\n")
                        song.thumbnailUrl?.let { sb.append("  thumbnailUrl: \"$it\"\n") }
                        song.previewUrl?.let { sb.append("  previewUrl: \"$it\"\n") }
                        song.previewUrlProvider?.let { sb.append("  previewUrlProvider: \"$it\"\n") }
                    }
                    sb.append("---\n\n")

                    sb.append("# ${journal.title}\n\n")
                    sb.append(journal.content)

                    if (journal.images.isNotEmpty()) {
                        sb.append("\n\n## Media\n")
                        journal.images.forEach { imagePath ->
                            val file = File(imagePath)
                            sb.append("![Media](../media/${journal.id}/${file.name})\n")
                        }
                    }

                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone(ZoneId.systemDefault())
                    val formattedDate = formatter.format(Instant.ofEpochMilli(journal.dateTime))

                    val sanitizedTitle = journal.title.replace("[\\\\/:*?\"<>|\\s]+".toRegex(), "_").trim()
                    val fileName = if (sanitizedTitle.isNotEmpty()) {
                        "${formattedDate}_${sanitizedTitle}_${journal.id}.md"
                    } else {
                        "${formattedDate}_${journal.id}.md"
                    }
                    val journalEntry = ZipEntry("journals/$fileName")
                    zos.putNextEntry(journalEntry)
                    zos.write(sb.toString().toByteArray())
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
                    AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Packed $packedMediaCount media files for markdown export")
                }
            }

            AppLogger.d(
                AppLogger.Category.BACKUP,
                "ExportImpl",
                "Markdown export completed successfully. Created zip: ${backupFile.name} (size: ${backupFile.length()} bytes)"
            )
            backupFile
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Markdown export failed with exception", e)
            null
        }
    }
}