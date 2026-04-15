package com.denser.june.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.preferences.JournalPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

class JournalPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : JournalPreferences {

    private companion object {
        val AUTO_TIME_ENABLED = booleanPreferencesKey("auto_time_enabled")
        val START_OF_WEEK = stringPreferencesKey("start_of_week")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val REMINDER_DAYS = stringPreferencesKey("reminder_days")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        const val DEFAULT_REMINDER_TIME = "21:14"
    }

    override fun isAutoTimeEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[AUTO_TIME_ENABLED] ?: false }

    override suspend fun setAutoTimeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_TIME_ENABLED] = enabled
        }
    }

    override fun startOfWeek(): Flow<DayOfWeek> = dataStore.data
        .map { preferences ->
            val value = preferences[START_OF_WEEK] ?: DayOfWeek.SUNDAY.name
            try {
                DayOfWeek.valueOf(value)
            } catch (e: Exception) {
                DayOfWeek.SUNDAY
            }
        }

    override suspend fun setStartOfWeek(dayOfWeek: DayOfWeek) {
        dataStore.edit { preferences ->
            preferences[START_OF_WEEK] = dayOfWeek.name
        }
    }
    override fun isReminderEnabled(): Flow<Boolean> = dataStore.data
        .map { it[REMINDER_ENABLED] ?: false }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    override fun reminderTime(): Flow<String> = dataStore.data
        .map { it[REMINDER_TIME] ?: DEFAULT_REMINDER_TIME }

    override suspend fun setReminderTime(time: String) {
        dataStore.edit { it[REMINDER_TIME] = time }
    }

    override fun reminderDays(): Flow<Set<DayOfWeek>> = dataStore.data
        .map { preferences ->
            val value = preferences[REMINDER_DAYS] ?: DayOfWeek.entries.joinToString(",") { it.name }
            value.split(",")
                .filter { it.isNotBlank() }
                .mapNotNull {
                    try { DayOfWeek.valueOf(it) } catch (e: Exception) { null }
                }.toSet()
        }

    override suspend fun setReminderDays(days: Set<DayOfWeek>) {
        dataStore.edit { it[REMINDER_DAYS] = days.joinToString(",") { it.name } }
    }

    override fun timeFormat(): Flow<TimeFormat> = dataStore.data
        .map { preferences ->
            val value = preferences[TIME_FORMAT] ?: TimeFormat.TWELVE_HOUR.name
            try {
                TimeFormat.valueOf(value)
            } catch (e: Exception) {
                TimeFormat.TWELVE_HOUR
            }
        }

    override suspend fun setTimeFormat(format: TimeFormat) {
        dataStore.edit { preferences ->
            preferences[TIME_FORMAT] = format.name
        }
    }
}
