package com.denser.june.notification.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.reminder.ReminderScheduler
import com.denser.june.notification.NotificationsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val journalPreferences: JournalPreferences by inject()
    private val reminderScheduler: ReminderScheduler by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationsHelper = NotificationsHelper(context)

        when (action) {
            "com.denser.june.ACTION_SHOW_REMINDER" -> {
                notificationsHelper.showReminderNotification()
                scope.launch {
                    val enabled = journalPreferences.isReminderEnabled().first()
                    if (enabled) {
                        val timeStr = journalPreferences.reminderTime().first()
                        val days = journalPreferences.reminderDays().first()
                        val time = LocalTime.parse(timeStr)
                        reminderScheduler.scheduleReminder(time, days)
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                scope.launch {
                    val enabled = journalPreferences.isReminderEnabled().first()
                    if (enabled) {
                        val timeStr = journalPreferences.reminderTime().first()
                        val days = journalPreferences.reminderDays().first()
                        val time = LocalTime.parse(timeStr)
                        reminderScheduler.scheduleReminder(time, days)
                    }
                }
            }
        }
    }
}
