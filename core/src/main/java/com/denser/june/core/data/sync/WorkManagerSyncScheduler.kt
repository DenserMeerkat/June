package com.denser.june.core.data.sync

import android.content.Context
import androidx.work.WorkManager
import com.denser.june.core.domain.sync.SyncScheduler

class WorkManagerSyncScheduler(private val context: Context) : SyncScheduler {
    override fun enqueue(onlyWifi: Boolean, immediate: Boolean, isFullRevalidation: Boolean) {
        SyncWorker.enqueue(context, onlyWifi, immediate = immediate, isFullRevalidation = isFullRevalidation)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}
