package com.denser.june.core.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.denser.june.core.domain.reminder.ReminderScheduler
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ReminderSchedulerImpl(
    private val context: Context
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleReminder(time: LocalTime, days: Set<DayOfWeek>) {
        if (days.isEmpty()) {
            cancelReminder()
            return
        }

        val nextTriggerTime = calculateNextTriggerTime(time, days)
        val intent = Intent("com.denser.june.ACTION_SHOW_REMINDER").apply {
            setPackage(context.packageName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime,
                pendingIntent
            )
        }
    }

    override fun cancelReminder() {
        val intent = Intent("com.denser.june.ACTION_SHOW_REMINDER").apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNextTriggerTime(time: LocalTime, days: Set<DayOfWeek>): Long {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayTime = today.atTime(time)

        if (days.contains(today.dayOfWeek) && todayTime.isAfter(now)) {
            return todayTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        for (i in 1..7) {
            val nextDate = today.plusDays(i.toLong())
            if (days.contains(nextDate.dayOfWeek)) {
                return nextDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }

        return todayTime.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    companion object {
        private const val REMINDER_ID = 1001
    }
}
