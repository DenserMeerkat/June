package com.denser.june.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.denser.june.core.domain.model.enums.AutoBackupInterval
import com.denser.june.core.domain.preferences.AutoBackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AutoBackupPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : AutoBackupPreferences {

    companion object {
        private val autoBackupEnabled = booleanPreferencesKey("auto_backup_enabled")
        private val folderUri = stringPreferencesKey("auto_backup_folder_uri")
        private val interval = stringPreferencesKey("auto_backup_interval")
        private val retentionCount = intPreferencesKey("auto_backup_retention_count")
        private val requireCharging = booleanPreferencesKey("auto_backup_require_charging")
        private val lastAutoBackupTime = longPreferencesKey("auto_backup_last_time")
        private val includeMedia = booleanPreferencesKey("auto_backup_include_media")
        private val includeAudios = booleanPreferencesKey("auto_backup_include_audios")
        private val includeSongs = booleanPreferencesKey("auto_backup_include_songs")
        private val includeSettings = booleanPreferencesKey("auto_backup_include_settings")
    }

    override fun isAutoBackupEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[autoBackupEnabled] ?: false }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[autoBackupEnabled] = enabled }
    }

    override fun getFolderUri(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[folderUri] }

    override suspend fun setFolderUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[folderUri] = uri
            } else {
                preferences.remove(folderUri)
            }
        }
    }

    override fun getInterval(): Flow<AutoBackupInterval> = dataStore.data
        .map { preferences -> AutoBackupInterval.fromName(preferences[interval]) }

    override suspend fun setInterval(interval: AutoBackupInterval) {
        dataStore.edit { preferences -> preferences[Companion.interval] = interval.name }
    }

    override fun getRetentionCount(): Flow<Int> = dataStore.data
        .map { preferences -> preferences[retentionCount] ?: 3 }

    override suspend fun setRetentionCount(count: Int) {
        dataStore.edit { preferences -> preferences[retentionCount] = count }
    }

    override fun isRequireCharging(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[requireCharging] ?: false }

    override suspend fun setRequireCharging(require: Boolean) {
        dataStore.edit { preferences -> preferences[requireCharging] = require }
    }

    override fun getLastAutoBackupTime(): Flow<Long> = dataStore.data
        .map { preferences -> preferences[lastAutoBackupTime] ?: 0L }

    override suspend fun setLastAutoBackupTime(timestamp: Long) {
        dataStore.edit { preferences -> preferences[lastAutoBackupTime] = timestamp }
    }

    override fun getIncludeMedia(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[includeMedia] ?: true }

    override suspend fun setIncludeMedia(include: Boolean) {
        dataStore.edit { preferences -> preferences[includeMedia] = include }
    }

    override fun getIncludeAudios(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[includeAudios] ?: true }

    override suspend fun setIncludeAudios(include: Boolean) {
        dataStore.edit { preferences -> preferences[includeAudios] = include }
    }

    override fun getIncludeSongs(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[includeSongs] ?: true }

    override suspend fun setIncludeSongs(include: Boolean) {
        dataStore.edit { preferences -> preferences[includeSongs] = include }
    }

    override fun getIncludeSettings(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[includeSettings] ?: true }

    override suspend fun setIncludeSettings(include: Boolean) {
        dataStore.edit { preferences -> preferences[includeSettings] = include }
    }
}

