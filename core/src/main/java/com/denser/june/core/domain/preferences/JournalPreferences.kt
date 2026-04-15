package com.denser.june.core.domain.preferences

import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface JournalPreferences {
    fun isAutoTimeEnabled(): Flow<Boolean>
    suspend fun setAutoTimeEnabled(enabled: Boolean)

    fun startOfWeek(): Flow<DayOfWeek>
    suspend fun setStartOfWeek(dayOfWeek: DayOfWeek)
}
