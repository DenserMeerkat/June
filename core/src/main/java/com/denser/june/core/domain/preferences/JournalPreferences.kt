package com.denser.june.core.domain.preferences

import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.model.enums.MapStyleProvider
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface JournalPreferences {
    fun isAutoTimeEnabled(): Flow<Boolean>
    suspend fun setAutoTimeEnabled(enabled: Boolean)

    fun startOfWeek(): Flow<DayOfWeek>
    suspend fun setStartOfWeek(dayOfWeek: DayOfWeek)

    fun isReminderEnabled(): Flow<Boolean>
    suspend fun setReminderEnabled(enabled: Boolean)

    fun reminderTime(): Flow<String>
    suspend fun setReminderTime(time: String)

    fun reminderDays(): Flow<Set<DayOfWeek>>
    suspend fun setReminderDays(days: Set<DayOfWeek>)

    fun timeFormat(): Flow<TimeFormat>
    suspend fun setTimeFormat(format: TimeFormat)

    fun mapTheme(): Flow<MapTheme>
    suspend fun setMapTheme(theme: MapTheme)

    fun isMarkdownEnabled(): Flow<Boolean>
    suspend fun setMarkdownEnabled(enabled: Boolean)

    fun mapStyleProvider(): Flow<MapStyleProvider>
    suspend fun setMapStyleProvider(provider: MapStyleProvider)

    fun maptilerKey(): Flow<String>
    suspend fun setMaptilerKey(key: String)

    fun stadiaKey(): Flow<String>
    suspend fun setStadiaKey(key: String)

    fun mapboxkey(): Flow<String>
    suspend fun setMapboxkey(key: String)

    fun isMapProviderVerified(provider: MapStyleProvider): Flow<Boolean>
    suspend fun setMapProviderVerified(provider: MapStyleProvider, verified: Boolean)
}
