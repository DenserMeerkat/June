package com.denser.june.core.domain.preferences

import com.denser.june.core.domain.model.enums.AutoBackupInterval
import kotlinx.coroutines.flow.Flow

interface AutoBackupPreferences {
    fun isAutoBackupEnabled(): Flow<Boolean>
    suspend fun setAutoBackupEnabled(enabled: Boolean)

    fun getFolderUri(): Flow<String?>
    suspend fun setFolderUri(uri: String?)

    fun getInterval(): Flow<AutoBackupInterval>
    suspend fun setInterval(interval: AutoBackupInterval)

    fun getRetentionCount(): Flow<Int>
    suspend fun setRetentionCount(count: Int)

    fun isRequireCharging(): Flow<Boolean>
    suspend fun setRequireCharging(require: Boolean)

    fun getLastAutoBackupTime(): Flow<Long>
    suspend fun setLastAutoBackupTime(timestamp: Long)

    fun getIncludeMedia(): Flow<Boolean>
    suspend fun setIncludeMedia(include: Boolean)

    fun getIncludeAudios(): Flow<Boolean>
    suspend fun setIncludeAudios(include: Boolean)

    fun getIncludeSongs(): Flow<Boolean>
    suspend fun setIncludeSongs(include: Boolean)

    fun getIncludeSettings(): Flow<Boolean>
    suspend fun setIncludeSettings(include: Boolean)
}

