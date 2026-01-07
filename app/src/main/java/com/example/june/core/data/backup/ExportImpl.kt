package com.example.june.core.data.backup

import android.content.Context
import android.util.Log
import com.example.june.core.domain.JournalRepo
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.ExportSchema
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
    private val journalRepo: JournalRepo,
    private val context: Context
) : ExportRepo {

    override suspend fun exportData(includeMedia: Boolean): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val journals = journalRepo.getAllJournals()

            Log.d("ExportDebug", "Found ${journals.size} journals to export")

            val jsonString = Json.Default.encodeToString(
                ExportSchema(
                    schemaVersion = 1,
                    journals = journals
                )
            )

            Log.d("ExportDebug", "Generated JSON string length: ${jsonString.length}")

            val backupFile = File(context.cacheDir, "JuneBackup_${System.currentTimeMillis()}.zip")
            val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile)))

            zipOutputStream.use { zos ->
                val jsonEntry = ZipEntry("journal_data.json")
                zos.putNextEntry(jsonEntry)
                zos.write(jsonString.toByteArray())
                zos.closeEntry()

                if (includeMedia) {
                    val processedFileNames = mutableSetOf<String>()

                    journals.flatMap { it.images }.forEach { absolutePath ->
                        val file = File(absolutePath)
                        if (file.exists() && processedFileNames.add(file.name)) {
                            try {
                                val mediaEntry = ZipEntry("media/${file.name}")
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

            backupFile
        } catch (e: Exception) {
            Log.wtf("ExportImpl", e)
            null
        }
    }
}