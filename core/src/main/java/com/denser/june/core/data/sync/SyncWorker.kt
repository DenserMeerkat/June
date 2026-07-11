package com.denser.june.core.data.sync

import android.content.Context
import androidx.work.*
import com.denser.june.core.domain.sync.SyncManager
import com.denser.june.core.domain.logging.AppLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val syncManager: SyncManager by inject()

    override suspend fun doWork(): Result {
        val notificationHelper = SyncNotificationHelper(applicationContext)

        return coroutineScope {
            val statusCollector = launch {
                syncManager.status.collect { status ->
                    notificationHelper.updateProgress(status)
                }
            }

            try {
                val isFullRevalidation = inputData.getBoolean("is_full_revalidation", false)
                AppLogger.d(AppLogger.Category.SYNC, "SyncWorker", "Starting SyncWorker doWork(). Full revalidation: $isFullRevalidation")
                val result = syncManager.sync(isFullRevalidation)
                statusCollector.cancel()
                notificationHelper.cancelProgressNotification()

                if (result.isSuccess) {
                    AppLogger.d(AppLogger.Category.SYNC, "SyncWorker", "SyncWorker completed successfully.")
                    Result.success()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    AppLogger.w(AppLogger.Category.SYNC, "SyncWorker", "SyncWorker sync failed. Attempt: $runAttemptCount")
                    notificationHelper.showFailureNotification(errorMsg)
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            } catch (e: Exception) {
                statusCollector.cancel()
                notificationHelper.cancelProgressNotification()
                AppLogger.e(AppLogger.Category.SYNC, "SyncWorker", "SyncWorker encountered exception. Attempt: $runAttemptCount", e)
                notificationHelper.showFailureNotification(e.message ?: "Unknown exception")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "com.denser.june.sync_worker"
        private const val COALESCING_DELAY_SECONDS = 3L

        fun enqueue(
            context: Context,
            onlyWifi: Boolean,
            immediate: Boolean = false,
            isFullRevalidation: Boolean = false
        ) {
            AppLogger.d(AppLogger.Category.SYNC, "SyncWorker", "Enqueuing SyncWorker. onlyWifi: $onlyWifi, immediate: $immediate, isFullRevalidation: $isFullRevalidation")
            val networkType = if (onlyWifi && !immediate) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val inputData = Data.Builder()
                .putBoolean("is_full_revalidation", isFullRevalidation)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setInitialDelay(if (immediate) 0L else COALESCING_DELAY_SECONDS, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}