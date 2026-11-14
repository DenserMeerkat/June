package com.denser.june.core.data.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.denser.june.core.R

class AutoBackupNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AUTO_BACKUP_CHANNEL_ID,
                context.getString(R.string.auto_backup),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.auto_backup_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPendingIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        } ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showProgressNotification(progressPercent: Int, phase: String) {
        val pendingIntent = getPendingIntent()
        val notification = NotificationCompat.Builder(context, AUTO_BACKUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.auto_backup_running))
            .setContentText(phase)
            .setProgress(100, progressPercent.coerceIn(0, 100), progressPercent == 0)
            .setOngoing(true)
            .setSilent(true)
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()
        notificationManager.notify(AUTO_BACKUP_PROGRESS_NOTIFICATION_ID, notification)
    }

    fun cancelProgressNotification() {
        notificationManager.cancel(AUTO_BACKUP_PROGRESS_NOTIFICATION_ID)
    }

    fun showSuccessNotification(backupFileName: String) {
        cancelProgressNotification()
        val pendingIntent = getPendingIntent()
        val notification = NotificationCompat.Builder(context, AUTO_BACKUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.auto_backup_success_title))
            .setContentText(context.getString(R.string.auto_backup_success_msg, backupFileName))
            .setAutoCancel(true)
            .setSilent(true)
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()
        notificationManager.notify(AUTO_BACKUP_RESULT_NOTIFICATION_ID, notification)
    }

    fun showFailureNotification(errorMessage: String) {
        cancelProgressNotification()
        val pendingIntent = getPendingIntent()
        val notification = NotificationCompat.Builder(context, AUTO_BACKUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(context.getString(R.string.auto_backup_failed_title))
            .setContentText(errorMessage)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .build()
        notificationManager.notify(AUTO_BACKUP_RESULT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val AUTO_BACKUP_CHANNEL_ID = "auto_backup_channel"
        private const val AUTO_BACKUP_PROGRESS_NOTIFICATION_ID = 300
        private const val AUTO_BACKUP_RESULT_NOTIFICATION_ID = 301
    }
}

