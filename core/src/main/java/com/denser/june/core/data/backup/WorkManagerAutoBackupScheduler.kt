package com.denser.june.core.data.backup

import android.content.Context
import androidx.work.*
import com.denser.june.core.domain.backup.AutoBackupScheduler
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.model.enums.AutoBackupInterval
import java.util.concurrent.TimeUnit

class WorkManagerAutoBackupScheduler(
    private val context: Context
) : AutoBackupScheduler {

    override fun schedule(interval: AutoBackupInterval, requireCharging: Boolean) {
        AppLogger.d(
            AppLogger.Category.BACKUP,
            "AutoBackupScheduler",
            "Scheduling auto backup. Interval: ${interval.name} (${interval.days} days), requireCharging: $requireCharging"
        )

        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .setRequiresCharging(requireCharging)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            interval.days,
            TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancel() {
        AppLogger.d(AppLogger.Category.BACKUP, "AutoBackupScheduler", "Cancelling auto backup work")
        WorkManager.getInstance(context).cancelUniqueWork(AutoBackupWorker.WORK_NAME)
    }
}

