package com.denser.june.presentation.screens.settings.screens.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

data class ReminderSettingsState(
    val isEnabled: Boolean = false,
    val time: String = "20:00",
    val days: Set<DayOfWeek> = DayOfWeek.values().toSet()
)

class ReminderVM(
    private val journalPreferences: JournalPreferences,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val state = combine(
        journalPreferences.isReminderEnabled(),
        journalPreferences.reminderTime(),
        journalPreferences.reminderDays()
    ) { enabled, time, days ->
        ReminderSettingsState(enabled, time, days)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderSettingsState()
    )

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            journalPreferences.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleReminder(LocalTime.parse(state.value.time), state.value.days)
            } else {
                reminderScheduler.cancelReminder()
            }
        }
    }

    fun updateTime(time: LocalTime) {
        viewModelScope.launch {
            val timeStr = time.toString()
            journalPreferences.setReminderTime(timeStr)
            if (state.value.isEnabled) {
                reminderScheduler.scheduleReminder(time, state.value.days)
            }
        }
    }

    fun toggleDay(day: DayOfWeek) {
        viewModelScope.launch {
            val currentDays = state.value.days
            val newDays = if (currentDays.contains(day)) {
                currentDays - day
            } else {
                currentDays + day
            }
            journalPreferences.setReminderDays(newDays)
            if (state.value.isEnabled) {
                reminderScheduler.scheduleReminder(LocalTime.parse(state.value.time), newDays)
            }
        }
    }
}
