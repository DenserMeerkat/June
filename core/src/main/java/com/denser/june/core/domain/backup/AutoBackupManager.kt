package com.denser.june.core.domain.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.denser.june.core.data.backup.AutoBackupNotificationHelper
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.preferences.AutoBackupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoBackupManager(
    private val context: Context,
    private val exportRepo: ExportRepo,
    private val prefs: AutoBackupPreferences,
    private val notificationHelper: AutoBackupNotificationHelper
) {
    private val backupMutex = Mutex()

    private val _status = MutableStateFlow<BackupOperationStatus>(BackupOperationStatus.Idle)
    val status: StateFlow<BackupOperationStatus> = _status.asStateFlow()

    suspend fun performAutoBackup(ignoreEnabledCheck: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        if (!backupMutex.tryLock()) {
            AppLogger.w(AppLogger.Category.BACKUP, "AutoBackupManager", "Auto backup is already running. Skipping concurrent run.")
            return@withContext Result.failure(IllegalStateException("Auto backup is already in progress"))
        }

        try {
            if (!ignoreEnabledCheck) {
                val isEnabled = prefs.isAutoBackupEnabled().first()
                if (!isEnabled) {
                    AppLogger.d(AppLogger.Category.BACKUP, "AutoBackupManager", "Auto backup is disabled. Skipping.")
                    return@withContext Result.failure(IllegalStateException("Auto backup is disabled"))
                }
            }

            val folderUriString = prefs.getFolderUri().first()
            if (folderUriString.isNullOrBlank()) {
                val errorMsg = "Backup folder not selected"
                AppLogger.e(AppLogger.Category.BACKUP, "AutoBackupManager", errorMsg)
                notificationHelper.showFailureNotification(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            val folderUri = Uri.parse(folderUriString)
            val dirDoc = DocumentFile.fromTreeUri(context, folderUri)
            if (dirDoc == null || !dirDoc.exists() || !dirDoc.canWrite()) {
                val errorMsg = "Cannot write to backup folder. Folder permission may have expired."
                AppLogger.e(AppLogger.Category.BACKUP, "AutoBackupManager", errorMsg)
                notificationHelper.showFailureNotification(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            _status.value = BackupOperationStatus.InProgress(
                phase = "Preparing",
                current = 0,
                total = 100,
                percentage = 0f,
                currentItemName = ""
            )
            notificationHelper.showProgressNotification(0, "Preparing…")

            val includeMedia = prefs.getIncludeMedia().first()
            val includeAudios = prefs.getIncludeAudios().first()
            val includeSongs = prefs.getIncludeSongs().first()
            val includeSettings = prefs.getIncludeSettings().first()

            val exportResult = exportRepo.exportData(
                includeMedia = includeMedia,
                includeAudios = includeAudios,
                includeSongFiles = includeSongs,
                includeSettings = includeSettings,
                includeMapKeys = false,
                onProgress = { status ->
                    _status.value = status
                    val percent = (status.percentage * 100).toInt()
                    val text = if (status.total > 0) {
                        "(${status.current}/${status.total}) ${status.phase}"
                    } else {
                        status.phase
                    }
                    notificationHelper.showProgressNotification(percent, text)
                }
            )

            if (exportResult.isFailure) {
                val error = exportResult.exceptionOrNull()
                val errorMsg = error?.message ?: "Export failed"
                AppLogger.e(AppLogger.Category.BACKUP, "AutoBackupManager", "Export failed", error)
                notificationHelper.showFailureNotification(errorMsg)
                _status.value = BackupOperationStatus.Error(errorMsg, error)
                return@withContext Result.failure(error ?: Exception(errorMsg))
            }

            val tempFile: File = exportResult.getOrThrow()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "June_Auto_Backup_${dateFormat.format(Date())}.zip"

            _status.value = BackupOperationStatus.InProgress(
                phase = "Saving",
                current = 95,
                total = 100,
                percentage = 0.95f,
                currentItemName = fileName
            )
            notificationHelper.showProgressNotification(95, "Saving…")

            val backupDoc = dirDoc.createFile("application/zip", fileName)
            if (backupDoc == null) {
                tempFile.delete()
                val errorMsg = "Failed to create backup file in destination folder"
                AppLogger.e(AppLogger.Category.BACKUP, "AutoBackupManager", errorMsg)
                notificationHelper.showFailureNotification(errorMsg)
                _status.value = BackupOperationStatus.Error(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            val writeSuccessful = try {
                context.contentResolver.openOutputStream(backupDoc.uri)?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Category.BACKUP, "AutoBackupManager", "Failed writing backup to SAF uri", e)
                backupDoc.delete()
                false
            } finally {
                tempFile.delete()
            }

            if (!writeSuccessful) {
                val errorMsg = "Failed writing backup file to destination"
                notificationHelper.showFailureNotification(errorMsg)
                _status.value = BackupOperationStatus.Error(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            // Pruning old backups
            val retentionCount = prefs.getRetentionCount().first()
            if (retentionCount > 0) {
                pruneOldBackups(dirDoc, retentionCount)
            }

            prefs.setLastAutoBackupTime(System.currentTimeMillis())
            notificationHelper.showSuccessNotification(fileName)
            AppLogger.d(AppLogger.Category.BACKUP, "AutoBackupManager", "Auto backup completed successfully: $fileName")
            _status.value = BackupOperationStatus.Success("Backup created successfully")
            Result.success(fileName)
        } finally {
            _status.value = BackupOperationStatus.Idle
            backupMutex.unlock()
        }
    }

    private fun pruneOldBackups(directory: DocumentFile, maxCount: Int) {
        try {
            val autoBackups = directory.listFiles()
                .filter { doc ->
                    val name = doc.name ?: ""
                    name.startsWith(AUTO_BACKUP_PREFIX) && name.endsWith(".zip")
                }
                .sortedBy { it.name }

            if (autoBackups.size > maxCount) {
                val toDelete = autoBackups.take(autoBackups.size - maxCount)
                toDelete.forEach { doc ->
                    AppLogger.d(AppLogger.Category.BACKUP, "AutoBackupManager", "Pruning old auto backup: ${doc.name}")
                    doc.delete()
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, "AutoBackupManager", "Failed to prune old auto backups", e)
        }
    }

    companion object {
        const val AUTO_BACKUP_PREFIX = "June_Auto_Backup_"
    }
}

