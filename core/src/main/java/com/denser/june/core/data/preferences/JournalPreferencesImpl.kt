package com.denser.june.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
}
