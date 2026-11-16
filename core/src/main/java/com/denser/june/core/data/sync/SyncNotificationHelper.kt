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
                context.getString(R.string.cloud_sync),
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

    private var lastProgressNotifyTime = 0L
    private var lastReportedPercent = -1

    fun showProgressNotification(progressPercent: Int, phaseText: String, isIndeterminate: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!isIndeterminate && progressPercent < 100) {
            if (now - lastProgressNotifyTime < 500L && progressPercent == lastReportedPercent) {
                return
            }
            if (now - lastProgressNotifyTime < 350L) {
                return
            }
        }
        lastProgressNotifyTime = now
        lastReportedPercent = progressPercent

        val pendingIntent = getPendingIntent()
        val stopPendingIntent = getStopPendingIntent()
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.cloud_sync))
            .setContentText(phaseText)
            .setSubText(if (!isIndeterminate) "$progressPercent%" else null)
            .setProgress(100, progressPercent.coerceIn(0, 100), isIndeterminate)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.close_24px,
                context.getString(R.string.stop_sync),
                stopPendingIntent
            )
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()
        notificationManager.notify(SYNC_PROGRESS_NOTIFICATION_ID, notification)
    }

    fun updateProgress(status: SyncStatus) {
        when (status) {
            is SyncStatus.Preparing -> {
                lastReportedPercent = -1
                showProgressNotification(0, "Preparing…", isIndeterminate = true)
            }
            is SyncStatus.Syncing -> {
                val progressPercent = (status.progress * 100).toInt().coerceIn(0, 100)
                val completed = status.uploadCount + status.downloadCount
                val phaseText = if (status.totalOperations > 0) {
                    "($completed/${status.totalOperations}) ${status.currentOperation}"
                } else {
                    status.currentOperation
                }
                showProgressNotification(progressPercent, phaseText, isIndeterminate = false)
            }
            is SyncStatus.Success -> {
                showSuccessNotification()
            }
            is SyncStatus.Error -> {
                showFailureNotification(status.message)
            }
            else -> {
                // Idle or other states
            }
        }
    }

    fun cancelProgressNotification() {
        notificationManager.cancel(SYNC_PROGRESS_NOTIFICATION_ID)
    }

    fun showSuccessNotification(message: String? = null) {
        cancelProgressNotification()
        val pendingIntent = getPendingIntent()
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Cloud Sync Complete")
            .setContentText(message ?: "All journals and attachments are up to date")
            .setAutoCancel(true)
            .setSilent(true)
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()
        notificationManager.notify(SYNC_RESULT_NOTIFICATION_ID, notification)
    }

    fun showFailureNotification(errorMessage: String) {
        cancelProgressNotification()
        val pendingIntent = getPendingIntent()
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("Cloud Sync Failed")
            .setContentText(errorMessage)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()
        notificationManager.notify(SYNC_RESULT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val SYNC_CHANNEL_ID = "sync_progress_channel"
        private const val SYNC_PROGRESS_NOTIFICATION_ID = 200
        private const val SYNC_RESULT_NOTIFICATION_ID = 201
    }
}
