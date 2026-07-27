package com.denser.june.notification.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.denser.june.core.domain.sync.SyncManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StopSyncReceiver : BroadcastReceiver(), KoinComponent {

    private val syncManager: SyncManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_SYNC) {
            syncManager.cancelSync()
        }
    }

    companion object {
        const val ACTION_STOP_SYNC = "com.denser.june.ACTION_STOP_SYNC"
    }
}
