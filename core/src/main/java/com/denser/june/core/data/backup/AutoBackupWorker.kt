package com.denser.june.core.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.denser.june.core.domain.backup.AutoBackupManager
import com.denser.june.core.domain.logging.AppLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val autoBackupManager: AutoBackupManager by inject()

    override suspend fun doWork(): Result {
        AppLogger.d(AppLogger.Category.BACKUP, "AutoBackupWorker", "Starting AutoBackupWorker. Attempt: $runAttemptCount")
        return try {
            val result = autoBackupManager.performAutoBackup()
            if (result.isSuccess) {
                AppLogger.d(AppLogger.Category.BACKUP, "AutoBackupWorker", "AutoBackupWorker completed successfully.")
                Result.success()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                AppLogger.w(AppLogger.Category.BACKUP, "AutoBackupWorker", "AutoBackupWorker failed: $errorMsg. Attempt: $runAttemptCount")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BACKUP, "AutoBackupWorker", "AutoBackupWorker encountered exception. Attempt: $runAttemptCount", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        internal const val WORK_NAME = "com.denser.june.auto_backup_worker"
    }
}

