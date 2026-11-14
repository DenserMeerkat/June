package com.denser.june.core.data.backup

import android.content.Context
import com.denser.june.core.data.database.journal.JournalDatabase
import com.denser.june.core.domain.backup.BackupOperationStatus
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.PreBackupCounts
import com.denser.june.core.domain.backup.SettingsBackupRepo
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.sync.JournalSyncMeta
import com.denser.june.core.domain.sync.SyncManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    private val settingsBackupRepo: SettingsBackupRepo,
    private val context: Context
) : ExportRepo {

    override suspend fun getPreBackupCounts(): PreBackupCounts = withContext(Dispatchers.IO) {
        val journals = journalRepo.getAllJournals()
        val mediaCount = journals.flatMap { it.images }.map { File(it).name }.distinct().size
        val audiosCount = journals.flatMap { it.audios }.map { File(it.path).name }.distinct().size
        val songCount = journals.mapNotNull { it.songDetails }.flatMap {
            listOfNotNull(it.localPreviewPath?.let { p -> File(p).name }, it.localThumbnailPath?.let { p -> File(p).name })
        }.distinct().size
        val hasMapKeys = runCatching { settingsBackupRepo.hasExportableMapKeys().first() }.getOrDefault(false)

        PreBackupCounts(
            totalJournals = journals.size,
            totalMedia = mediaCount,
            totalAudios = audiosCount,
            totalSongs = songCount,
            hasSettings = true,
            hasMapKeys = hasMapKeys
        )
    }

    override suspend fun exportData(
        includeMedia: Boolean,
        includeAudios: Boolean,
        includeSongFiles: Boolean,
        includeSettings: Boolean,
        includeMapKeys: Boolean,
        onProgress: ((BackupOperationStatus.InProgress) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            AppLogger.d(
                AppLogger.Category.BACKUP,
                "ExportImpl",
                "Starting export process. Include media: $includeMedia, includeAudios: $includeAudios, includeSongFiles: $includeSongFiles, includeSettings: $includeSettings, includeMapKeys: $includeMapKeys"
            )
            val journals = journalRepo.getAllJournals()

            AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Found ${journals.size} journals to export")

            val mediaDir = File(context.filesDir, "media")
            val audioDir = File(context.filesDir, "audios")
            val songLibraryDir = File(context.filesDir, "songs/library")
            val songArtDir = File(context.filesDir, "songs/art")

            val cleanedJournals = journals.map { journal ->
                val cleanedImages = journal.images.map { path ->
                    val file = File(path)
                    File(mediaDir, file.name).absolutePath
                }
                val cleanedAudios = journal.audios.map { audio ->
                    val file = File(audio.path)
                    audio.copy(path = File(audioDir, file.name).absolutePath)
                }
                val cleanedSong = journal.songDetails?.copy(
                    localPreviewPath = journal.songDetails.localPreviewPath?.let { File(songLibraryDir, File(it).name).absolutePath },
                    localThumbnailPath = journal.songDetails.localThumbnailPath?.let { File(songArtDir, File(it).name).absolutePath }
                )
                journal.copy(images = cleanedImages, audios = cleanedAudios, songDetails = cleanedSong)
            }

            val journalMeta = cleanedJournals.associate { j ->
                j.id to JournalSyncMeta(rev = 1, contentHash = j.computeContentHash())
            }

            val totalMedia = if (includeMedia) {
                cleanedJournals.flatMap { it.images }.map { File(it).name }.distinct().size
            } else 0
            val totalAudios = if (includeAudios) {
                cleanedJournals.flatMap { it.audios }.map { File(it.path).name }.distinct().size
            } else 0
            val totalSongMedia = if (includeSongFiles) {
                cleanedJournals.mapNotNull { it.songDetails }.flatMap {
                    listOfNotNull(it.localPreviewPath?.let { p -> File(p).name }, it.localThumbnailPath?.let { p -> File(p).name })
                }.distinct().size
            } else 0

            val totalOps = 1 + cleanedJournals.size +
                    (if (includeMedia) totalMedia else 0) +
                    (if (includeAudios) totalAudios else 0) +
                    (if (includeSongFiles) totalSongMedia else 0) +
                    (if (includeSettings) 1 else 0)
            var currentOp = 0

            val manifest = SyncManifest(
                lastSyncTime = System.currentTimeMillis(),
                lastSyncDeviceId = "backup_export",
                databaseVersion = JournalDatabase.VERSION,
                schemaVersion = SyncManifest.CURRENT_SCHEMA_VERSION,
                totalJournals = cleanedJournals.size,
                totalMedia = totalMedia,
                totalSongMedia = totalSongMedia,
                totalAudio = totalAudios,
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

                currentOp++
                onProgress?.invoke(
                    BackupOperationStatus.InProgress(
                        phase = "Manifest",
                        current = currentOp,
                        total = totalOps,
                        percentage = currentOp.toFloat() / totalOps,
                        currentItemName = "manifest.json"
                    )
                )

                cleanedJournals.forEachIndexed { idx, journal ->
                    val sanitizedSong = journal.songDetails?.copy(
                        localPreviewPath = journal.songDetails.localPreviewPath?.let { File(it).name },
                        localThumbnailPath = journal.songDetails.localThumbnailPath?.let { File(it).name }
                    )
                    val sanitizedAudios = journal.audios.map { audio ->
                        audio.copy(path = File(audio.path).name)
                    }
                    val sanitizedJournal = journal.copy(
                        images = journal.images.map { File(it).name },
                        audios = sanitizedAudios,
                        songDetails = sanitizedSong
                    )
                    val journalJson = Json.Default.encodeToString(Journal.serializer(), sanitizedJournal)
                    val journalEntry = ZipEntry("journals/${journal.id}.json")
                    zos.putNextEntry(journalEntry)
                    zos.write(journalJson.toByteArray())
                    zos.closeEntry()

                    currentOp++
                    onProgress?.invoke(
                        BackupOperationStatus.InProgress(
                            phase = "Journals",
                            current = currentOp,
                            total = totalOps,
                            percentage = currentOp.toFloat() / totalOps,
                            currentItemName = journal.title.ifBlank { "Journal ${idx + 1}" }
                        )
                    )
                }

                if (includeMedia) {
                    val processedFileNames = mutableSetOf<String>()
                    var packedMediaCount = 0

                    cleanedJournals.forEach { journal ->
                        journal.images.forEach { absolutePath ->
                            val file = File(absolutePath)
                            if (file.exists() && processedFileNames.add(file.name)) {
                                try {
                                    val mediaEntry = ZipEntry("media/${file.name}")
                                    zos.putNextEntry(mediaEntry)

                                    FileInputStream(file).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                    packedMediaCount++

                                    currentOp++
                                    onProgress?.invoke(
                                        BackupOperationStatus.InProgress(
                                            phase = "Media",
                                            current = currentOp,
                                            total = totalOps,
                                            percentage = currentOp.toFloat() / totalOps,
                                            currentItemName = file.name
                                        )
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack media file: ${file.name}", e)
                                }
                            }
                        }
                    }
                    AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Packed $packedMediaCount media files")
                }

                if (includeAudios) {
                    val processedAudioNames = mutableSetOf<String>()
                    var packedAudioCount = 0

                    cleanedJournals.forEach { journal ->
                        journal.audios.forEach { audio ->
                            val file = File(audio.path)
                            if (file.exists() && processedAudioNames.add(file.name)) {
                                try {
                                    val audioEntry = ZipEntry("audios/${file.name}")
                                    zos.putNextEntry(audioEntry)
                                    FileInputStream(file).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                    packedAudioCount++

                                    currentOp++
                                    onProgress?.invoke(
                                        BackupOperationStatus.InProgress(
                                            phase = "Audios",
                                            current = currentOp,
                                            total = totalOps,
                                            percentage = currentOp.toFloat() / totalOps,
                                            currentItemName = file.name
                                        )
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack audio file: ${file.name}", e)
                                }
                            }
                        }
                    }
                    AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Packed $packedAudioCount audio files")
                }

                if (includeSongFiles) {
                    val processedSongNames = mutableSetOf<String>()
                    var packedSongCount = 0

                    cleanedJournals.mapNotNull { it.songDetails }.forEach { song ->
                        song.localPreviewPath?.let { path ->
                            val file = File(path)
                            if (file.exists() && processedSongNames.add("library/${file.name}")) {
                                try {
                                    val songEntry = ZipEntry("songs/library/${file.name}")
                                    zos.putNextEntry(songEntry)
                                    FileInputStream(file).use { it.copyTo(zos) }
                                    zos.closeEntry()
                                    packedSongCount++

                                    currentOp++
                                    onProgress?.invoke(
                                        BackupOperationStatus.InProgress(
                                            phase = "Songs",
                                            current = currentOp,
                                            total = totalOps,
                                            percentage = currentOp.toFloat() / totalOps,
                                            currentItemName = file.name
                                        )
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack song file: ${file.name}", e)
                                }
                            }
                        }
                        song.localThumbnailPath?.let { path ->
                            val file = File(path)
                            if (file.exists() && processedSongNames.add("art/${file.name}")) {
                                try {
                                    val artEntry = ZipEntry("songs/art/${file.name}")
                                    zos.putNextEntry(artEntry)
                                    FileInputStream(file).use { it.copyTo(zos) }
                                    zos.closeEntry()
                                    packedSongCount++

                                    currentOp++
                                    onProgress?.invoke(
                                        BackupOperationStatus.InProgress(
                                            phase = "Songs",
                                            current = currentOp,
                                            total = totalOps,
                                            percentage = currentOp.toFloat() / totalOps,
                                            currentItemName = file.name
                                        )
                                    )
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack song art: ${file.name}", e)
                                }
                            }
                        }
                    }
                    AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Packed $packedSongCount song files")
                }

                if (includeSettings) {
                    try {
                        val settingsJsonResult = settingsBackupRepo.exportSettingsJson(includeMapKeys)
                        val settingsJson = settingsJsonResult.getOrNull()
                        if (settingsJson != null) {
                            val settingsEntry = ZipEntry("settings.json")
                            zos.putNextEntry(settingsEntry)
                            zos.write(settingsJson.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                            AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Packed settings.json into backup zip")
                        }
                    } catch (e: Exception) {
                        AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack settings.json", e)
                    }

                    currentOp++
                    onProgress?.invoke(
                        BackupOperationStatus.InProgress(
                            phase = "Settings",
                            current = currentOp,
                            total = totalOps,
                            percentage = 1.0f,
                            currentItemName = "settings.json"
                        )
                    )
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

    override suspend fun exportAsMarkdown(
        includeMedia: Boolean,
        includeAudios: Boolean,
        includeSongFiles: Boolean,
        onProgress: ((BackupOperationStatus.InProgress) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            AppLogger.d(
                AppLogger.Category.BACKUP,
                "ExportImpl",
                "Starting markdown export. Include media: $includeMedia, audios: $includeAudios, songs: $includeSongFiles"
            )
            val journals = journalRepo.getAllJournals()

            AppLogger.d(AppLogger.Category.BACKUP, "ExportImpl", "Found ${journals.size} journals to export as markdown")

            val mediaDir = File(context.filesDir, "media")
            val audioDir = File(context.filesDir, "audios")
            val cleanedJournals = journals.map { journal ->
                val cleanedImages = journal.images.map { path ->
                    val file = File(path)
                    File(mediaDir, file.name).absolutePath
                }
                val cleanedAudios = journal.audios.map { audio ->
                    val file = File(audio.path)
                    audio.copy(path = File(audioDir, file.name).absolutePath)
                }
                journal.copy(images = cleanedImages, audios = cleanedAudios)
            }

            val backupFile = File(context.cacheDir, "JuneMarkdownExport_${System.currentTimeMillis()}.zip")
            val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile)))

            val totalOps = cleanedJournals.size
            var currentOp = 0

            val usedFileNames = mutableSetOf<String>()
            val processedSongNames = mutableSetOf<String>()
            zipOutputStream.use { zos ->
                cleanedJournals.forEach { journal ->
                    val mediaPrefix = if (includeMedia) "media/${journal.id}" else null
                    val audioPrefix = if (includeAudios) "audios/${journal.id}" else null
                    val songPrefix = if (includeSongFiles) "songs" else null

                    val markdownText = com.denser.june.core.domain.markdown.MarkdownEngine.toMarkdown(
                        journal = journal,
                        relativeMediaPathPrefix = mediaPrefix,
                        relativeAudioPathPrefix = audioPrefix,
                        relativeSongPathPrefix = songPrefix,
                        includeMedia = includeMedia,
                        includeAudios = includeAudios,
                        includeSongFiles = includeSongFiles
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
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack media file for markdown: ${file.name}", e)
                                }
                            }
                        }
                    }

                    if (includeAudios) {
                        journal.audios.forEach { audio ->
                            val file = File(audio.path)
                            if (file.exists()) {
                                try {
                                    val audioEntry = ZipEntry("audios/${journal.id}/${file.name}")
                                    zos.putNextEntry(audioEntry)
                                    FileInputStream(file).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack audio file for markdown: ${file.name}", e)
                                }
                            }
                        }
                    }

                    if (includeSongFiles) {
                        journal.songDetails?.localPreviewPath?.let { path ->
                            val file = File(path)
                            if (file.exists() && processedSongNames.add("songs/${file.name}")) {
                                try {
                                    val songEntry = ZipEntry("songs/${file.name}")
                                    zos.putNextEntry(songEntry)
                                    FileInputStream(file).use { it.copyTo(zos) }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack song file for markdown: ${file.name}", e)
                                }
                            }
                        }
                        journal.songDetails?.localThumbnailPath?.let { path ->
                            val file = File(path)
                            if (file.exists() && processedSongNames.add("songs/${file.name}")) {
                                try {
                                    val songEntry = ZipEntry("songs/${file.name}")
                                    zos.putNextEntry(songEntry)
                                    FileInputStream(file).use { it.copyTo(zos) }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack song art for markdown: ${file.name}", e)
                                }
                            }
                        }
                    }

                    currentOp++
                    onProgress?.invoke(
                        BackupOperationStatus.InProgress(
                            phase = "Markdown",
                            current = currentOp,
                            total = totalOps,
                            percentage = currentOp.toFloat() / totalOps,
                            currentItemName = fileName
                        )
                    )
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
                relativeAudioPathPrefix = "audios",
                relativeSongPathPrefix = "songs",
                includeMedia = true,
                includeAudios = true,
                includeSongFiles = true
            )

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val mdEntry = ZipEntry(fileName)
                zos.putNextEntry(mdEntry)
                zos.write(markdownText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                val processedFileNames = mutableSetOf<String>()
                journal.images.forEach { imagePath ->
                    val file = File(imagePath)
                    if (file.exists() && processedFileNames.add("media/${file.name}")) {
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

                journal.audios.forEach { audio ->
                    val file = File(audio.path)
                    if (file.exists() && processedFileNames.add("audios/${file.name}")) {
                        try {
                            val audioEntry = ZipEntry("audios/${file.name}")
                            zos.putNextEntry(audioEntry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack audio file for single export: ${file.name}", e)
                        }
                    }
                }

                journal.songDetails?.localPreviewPath?.let { path ->
                    val file = File(path)
                    if (file.exists() && processedFileNames.add("songs/${file.name}")) {
                        try {
                            val songEntry = ZipEntry("songs/${file.name}")
                            zos.putNextEntry(songEntry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            AppLogger.e(AppLogger.Category.BACKUP, "ExportImpl", "Failed to pack song file for single export: ${file.name}", e)
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
