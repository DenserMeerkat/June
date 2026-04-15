package com.denser.june.core.domain.reminder

import java.time.DayOfWeek
import java.time.LocalTime

interface ReminderScheduler {
    fun scheduleReminder(time: LocalTime, days: Set<DayOfWeek>)
    fun cancelReminder()
}
