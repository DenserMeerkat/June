package com.denser.june.core.data.backup

import android.content.Context
import android.util.Log
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.ExportSchema
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

class ExportImpl(
    private val journalRepo: JournalRepository,
    private val context: Context
) : ExportRepo {

    override suspend fun exportData(includeMedia: Boolean): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val journals = journalRepo.getAllJournals()

            Log.d("ExportDebug", "Found ${journals.size} journals to export")

            val mediaDir = File(context.filesDir, "journal_media")
            val cleanedJournals = journals.map { journal ->
                val cleanedImages = journal.images.map { path ->
                    val file = File(path)
                    File(mediaDir, file.name).absolutePath
                }
                journal.copy(images = cleanedImages)
            }

            val totalMedia = cleanedJournals.flatMap { it.images }.map { File(it).name }.distinct().size
            val manifest = SyncManifest(
                lastSyncTime = System.currentTimeMillis(),
                lastSyncDeviceId = "backup_export",
                databaseVersion = 4,
                schemaVersion = 2,
                totalJournals = cleanedJournals.size,
                totalMedia = totalMedia
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
                                } catch (e: Exception) {
                                    Log.e("ExportImpl", "Failed to pack file: $absolutePath", e)
                                }
                            }
                        }
                    }
                }
            }

            backupFile
        } catch (e: Exception) {
            Log.wtf("ExportImpl", e)
            null
        }
    }
}