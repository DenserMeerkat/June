package com.denser.june.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.model.enums.MapStyleProvider
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
        val MAP_THEME = stringPreferencesKey("map_theme")
        val MARKDOWN_ENABLED = booleanPreferencesKey("markdown_enabled")
        val MAP_STYLE_PROVIDER = stringPreferencesKey("map_style_provider")
        val MAPTILER_KEY = stringPreferencesKey("maptiler_key")
        val STADIA_KEY = stringPreferencesKey("stadia_key")
        val MAPBOX_KEY = stringPreferencesKey("mapbox_key")
        val MAPTILER_KEY_VERIFIED = booleanPreferencesKey("maptiler_key_verified")
        val STADIA_KEY_VERIFIED = booleanPreferencesKey("stadia_key_verified")
        val MAPBOX_KEY_VERIFIED = booleanPreferencesKey("mapbox_key_verified")
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

    override fun mapTheme(): Flow<MapTheme> = dataStore.data
        .map { preferences ->
            val value = preferences[MAP_THEME] ?: MapTheme.APP.name
            try {
                MapTheme.valueOf(value)
            } catch (e: Exception) {
                MapTheme.APP
            }
        }

    override suspend fun setMapTheme(theme: MapTheme) {
        dataStore.edit { preferences ->
            preferences[MAP_THEME] = theme.name
        }
    }

    override fun isMarkdownEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[MARKDOWN_ENABLED] ?: true }

    override suspend fun setMarkdownEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[MARKDOWN_ENABLED] = enabled
        }
    }

    override fun mapStyleProvider(): Flow<MapStyleProvider> = dataStore.data
        .map { preferences ->
            val value = preferences[MAP_STYLE_PROVIDER] ?: MapStyleProvider.CARTO.name
            try {
                MapStyleProvider.valueOf(value)
            } catch (e: Exception) {
                MapStyleProvider.CARTO
            }
        }

    override suspend fun setMapStyleProvider(provider: MapStyleProvider) {
        dataStore.edit { preferences ->
            preferences[MAP_STYLE_PROVIDER] = provider.name
        }
    }

    override fun maptilerKey(): Flow<String> = dataStore.data
        .map { preferences -> preferences[MAPTILER_KEY] ?: "" }

    override suspend fun setMaptilerKey(key: String) {
        dataStore.edit { preferences ->
            preferences[MAPTILER_KEY] = key
        }
    }

    override fun stadiaKey(): Flow<String> = dataStore.data
        .map { preferences -> preferences[STADIA_KEY] ?: "" }

    override suspend fun setStadiaKey(key: String) {
        dataStore.edit { preferences ->
            preferences[STADIA_KEY] = key
        }
    }

    override fun mapboxkey(): Flow<String> = dataStore.data
        .map { preferences -> preferences[MAPBOX_KEY] ?: "" }

    override suspend fun setMapboxkey(key: String) {
        dataStore.edit { preferences ->
            preferences[MAPBOX_KEY] = key
        }
    }

    override fun isMapProviderVerified(provider: MapStyleProvider): Flow<Boolean> = dataStore.data
        .map { preferences ->
            when (provider) {
                MapStyleProvider.CARTO -> true
                MapStyleProvider.MAPTILER -> preferences[MAPTILER_KEY_VERIFIED] ?: false
                MapStyleProvider.STADIA -> preferences[STADIA_KEY_VERIFIED] ?: false
                MapStyleProvider.MAPBOX -> preferences[MAPBOX_KEY_VERIFIED] ?: false
            }
        }

    override suspend fun setMapProviderVerified(provider: MapStyleProvider, verified: Boolean) {
        dataStore.edit { preferences ->
            when (provider) {
                MapStyleProvider.CARTO -> {}
                MapStyleProvider.MAPTILER -> preferences[MAPTILER_KEY_VERIFIED] = verified
                MapStyleProvider.STADIA -> preferences[STADIA_KEY_VERIFIED] = verified
                MapStyleProvider.MAPBOX -> preferences[MAPBOX_KEY_VERIFIED] = verified
            }
        }
    }
}
