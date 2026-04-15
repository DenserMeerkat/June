package com.denser.june.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.denser.june.MainActivity
import com.denser.june.core.R

class NotificationsHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Journaling reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification() {
        createNotificationChannel()

        val notificationOptions = listOf(
            "Time to write!" to "Don't forget to record your thoughts for today.",
            "Dear Diary..." to "Your journal is waiting for your story.",
            "Capture the moment" to "Consistency is key! Write a little bit today.",
            "How was your day?" to "What was the best part of your day?",
            "Daily Reflection" to "Take a moment to reflect on what happened today.",
            "Note to self" to "A few words now, a lifetime of memories later.",
            "Unwind and write" to "Reflect on today's journey before you rest."
        )

        val (title, message) = notificationOptions.random()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "reminder_channel"
        private const val NOTIFICATION_ID = 1
    }
}