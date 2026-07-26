package com.denser.june.core.data.sync

import android.content.Context
import com.denser.june.core.domain.sync.SyncScheduler

class WorkManagerSyncScheduler(private val context: Context) : SyncScheduler {
    override fun enqueue(onlyWifi: Boolean, immediate: Boolean, isFullRevalidation: Boolean) {
        SyncWorker.enqueue(context, onlyWifi, immediate = immediate, isFullRevalidation = isFullRevalidation)
    }
}
