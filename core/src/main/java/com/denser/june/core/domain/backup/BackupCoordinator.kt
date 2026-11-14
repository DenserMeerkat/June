package com.denser.june.core.domain.backup

import com.denser.june.core.domain.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class BackupCoordinator(
    private val exportRepo: ExportRepo,
    private val restoreRepo: RestoreRepo,
    private val appScope: CoroutineScope
) {
    private val _backupStatus = MutableStateFlow<BackupOperationStatus>(BackupOperationStatus.Idle)
    val backupStatus: StateFlow<BackupOperationStatus> = _backupStatus.asStateFlow()

    private val _restoreStatus = MutableStateFlow<BackupOperationStatus>(BackupOperationStatus.Idle)
    val restoreStatus: StateFlow<BackupOperationStatus> = _restoreStatus.asStateFlow()

    private var activeBackupJob: Job? = null
    private var activeRestoreJob: Job? = null

    suspend fun getPreBackupCounts(): PreBackupCounts {
        return exportRepo.getPreBackupCounts()
    }

    fun startBackup(
        includeMedia: Boolean = true,
        includeAudios: Boolean = true,
        includeSongFiles: Boolean = true,
        includeSettings: Boolean = true,
        includeMapKeys: Boolean = false,
        onComplete: ((Result<File>) -> Unit)? = null
    ): Job {
        activeBackupJob?.cancel()
        val job = appScope.launch(Dispatchers.IO) {
            _backupStatus.value = BackupOperationStatus.InProgress(
                phase = "Preparing",
                current = 0,
                total = 100,
                percentage = 0f,
                currentItemName = ""
            )
            AppLogger.d(AppLogger.Category.BACKUP, "BackupCoordinator", "Starting backup in ApplicationScope")
            val result = exportRepo.exportData(
                includeMedia = includeMedia,
                includeAudios = includeAudios,
                includeSongFiles = includeSongFiles,
                includeSettings = includeSettings,
                includeMapKeys = includeMapKeys,
                onProgress = { progress ->
                    _backupStatus.value = progress
                }
            )
            result.onSuccess { file ->
                AppLogger.d(AppLogger.Category.BACKUP, "BackupCoordinator", "Backup completed: ${file.name}")
                _backupStatus.value = BackupOperationStatus.Success("Backup created successfully")
                onComplete?.invoke(Result.success(file))
            }.onFailure { e ->
                AppLogger.e(AppLogger.Category.BACKUP, "BackupCoordinator", "Backup failed", e)
                _backupStatus.value = BackupOperationStatus.Error(e.localizedMessage ?: "Backup failed", e)
                onComplete?.invoke(Result.failure(e))
            }
        }
        activeBackupJob = job
        return job
    }

    fun startRestore(
        path: String,
        onComplete: ((Result<BackupSummary>) -> Unit)? = null
    ): Job {
        activeRestoreJob?.cancel()
        val job = appScope.launch(Dispatchers.IO) {
            _restoreStatus.value = BackupOperationStatus.InProgress(
                phase = "Preparing",
                current = 0,
                total = 100,
                percentage = 0f,
                currentItemName = ""
            )
            AppLogger.d(AppLogger.Category.BACKUP, "BackupCoordinator", "Starting restore in ApplicationScope")
            val result = restoreRepo.restoreData(
                path = path,
                onProgress = { progress ->
                    _restoreStatus.value = progress
                }
            )
            result.onSuccess { summary ->
                AppLogger.d(AppLogger.Category.BACKUP, "BackupCoordinator", "Restore succeeded: $summary")
                _restoreStatus.value = BackupOperationStatus.Success("Restore complete", summary)
                onComplete?.invoke(Result.success(summary))
            }.onFailure { e ->
                AppLogger.e(AppLogger.Category.BACKUP, "BackupCoordinator", "Restore failed", e)
                _restoreStatus.value = BackupOperationStatus.Error(e.localizedMessage ?: "Restore failed", e)
                onComplete?.invoke(Result.failure(e))
            }
        }
        activeRestoreJob = job
        return job
    }

    fun resetBackupStatus() {
        _backupStatus.value = BackupOperationStatus.Idle
    }

    fun resetRestoreStatus() {
        _restoreStatus.value = BackupOperationStatus.Idle
    }
}

