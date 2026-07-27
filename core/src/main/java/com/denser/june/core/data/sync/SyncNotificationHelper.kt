package com.denser.june.core.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncStatus

class SyncNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createSyncNotificationChannel()
    }

    private fun createSyncNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Sync Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the progress of cloud sync operations"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPendingIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SYNC_SETTINGS", true)
        } ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getStopPendingIntent(): PendingIntent {
        val intent = Intent("com.denser.june.ACTION_STOP_SYNC").apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateProgress(status: SyncStatus) {
        val pendingIntent = getPendingIntent()
        val stopPendingIntent = getStopPendingIntent()
        when (status) {
            is SyncStatus.Preparing -> {
                val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setContentTitle("Cloud Sync")
                    .setContentText("Preparing...")
                    .setProgress(100, 0, true)
                    .setOngoing(true)
                    .addAction(
                        R.drawable.close_24px,
                        "Stop",
                        stopPendingIntent
                    )
                    .apply {
                        if (pendingIntent != null) setContentIntent(pendingIntent)
                    }
                    .build()
                notificationManager.notify(SYNC_NOTIFICATION_ID, notification)
            }
            is SyncStatus.Syncing -> {
                val progressPercent = (status.progress * 100).toInt()
                val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setContentTitle("Cloud Sync")
                    .setContentText(status.currentOperation)
                    .setProgress(100, progressPercent, false)
                    .setOngoing(true)
                    .addAction(
                        R.drawable.close_24px,
                        "Stop",
                        stopPendingIntent
                    )
                    .apply {
                        if (pendingIntent != null) setContentIntent(pendingIntent)
                    }
                    .build()
                notificationManager.notify(SYNC_NOTIFICATION_ID, notification)
            }
            else -> {
                // Other states are not handled as active progress updates
            }
        }
    }

    fun cancelProgressNotification() {
        notificationManager.cancel(SYNC_NOTIFICATION_ID)
    }

    fun showFailureNotification(errorMessage: String) {
        val pendingIntent = getPendingIntent()
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Sync failed")
            .setContentText(errorMessage)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()
        notificationManager.notify(SYNC_FAILURE_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val SYNC_CHANNEL_ID = "sync_progress_channel"
        private const val SYNC_NOTIFICATION_ID = 200
        private const val SYNC_FAILURE_NOTIFICATION_ID = 201
    }
}
