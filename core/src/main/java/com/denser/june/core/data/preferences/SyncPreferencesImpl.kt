package com.denser.june.core.data.preferences
 
import java.util.UUID

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.denser.june.core.domain.preferences.SyncPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SyncPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : SyncPreferences {

    companion object {
        private val lastSyncTime = longPreferencesKey("last_sync_time")
        private val syncEnabled = booleanPreferencesKey("sync_enabled")
        private val selectedProvider = stringPreferencesKey("selected_provider")
        private val syncOnlyOnWifi = booleanPreferencesKey("sync_only_on_wifi")
        private val webDavUrl = stringPreferencesKey("webdav_url")
        private val webDavUsername = stringPreferencesKey("webdav_username")
        private val webDavPassword = stringPreferencesKey("webdav_password")
        private val automaticSyncEnabled = booleanPreferencesKey("automatic_sync_enabled")
        private val deviceId = stringPreferencesKey("device_id")
        private val gdSyncFolderId = stringPreferencesKey("gd_sync_folder_id")
        private val gdJournalsFolderId = stringPreferencesKey("gd_journals_folder_id")
        private val gdMediaFolderId = stringPreferencesKey("gd_media_folder_id")
        private val completedDataRepairVersion = intPreferencesKey("completed_data_repair_version")
        private val syncLoggingEnabled = booleanPreferencesKey("sync_logging_enabled")
        private val backupLoggingEnabled = booleanPreferencesKey("backup_logging_enabled")
        private val databaseLoggingEnabled = booleanPreferencesKey("database_logging_enabled")
        private val developerModeEnabled = booleanPreferencesKey("developer_mode_enabled")
    }

    override fun getLastSyncTime(): Flow<Long> = dataStore.data
        .map { preferences -> preferences[lastSyncTime] ?: 0L }

    override suspend fun setLastSyncTime(time: Long) {
        dataStore.edit { preferences ->
            preferences[lastSyncTime] = time
        }
    }

    override fun getSyncEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[syncEnabled] ?: false }

    override suspend fun setSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[syncEnabled] = enabled
        }
    }

    override fun getSelectedProvider(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[selectedProvider] }

    override suspend fun setSelectedProvider(providerName: String?) {
        dataStore.edit { preferences ->
            if (providerName != null) {
                preferences[selectedProvider] = providerName
            } else {
                preferences.remove(selectedProvider)
            }
        }
    }

    override fun getSyncOnlyOnWifi(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[syncOnlyOnWifi] ?: true }

    override suspend fun setSyncOnlyOnWifi(onlyWifi: Boolean) {
        dataStore.edit { preferences ->
            preferences[syncOnlyOnWifi] = onlyWifi
        }
    }

    override fun getWebDavUrl(): Flow<String?> = dataStore.data.map { it[webDavUrl] }
    override suspend fun setWebDavUrl(url: String?) {
        dataStore.edit { it.updateOrRemove(webDavUrl, url) }
    }

    override fun getWebDavUsername(): Flow<String?> = dataStore.data.map { it[webDavUsername] }
    override suspend fun setWebDavUsername(username: String?) {
        dataStore.edit { it.updateOrRemove(webDavUsername, username) }
    }

    override fun getWebDavPassword(): Flow<String?> = dataStore.data.map { it[webDavPassword] }
    override suspend fun setWebDavPassword(password: String?) {
        dataStore.edit { it.updateOrRemove(webDavPassword, password) }
    }

    override fun isAutomaticSyncEnabled(): Flow<Boolean> = dataStore.data.map { it[automaticSyncEnabled] ?: false }
    override suspend fun setAutomaticSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[automaticSyncEnabled] = enabled }
    }
 
    override suspend fun getDeviceId(): String {
        val current = dataStore.data.map { it[deviceId] }.first()
        return if (current != null) {
            current
        } else {
            val newId = UUID.randomUUID().toString()
            dataStore.edit { it[deviceId] = newId }
            newId
        }
    }

    override fun getGoogleDriveSyncFolderId(): Flow<String?> = dataStore.data.map { it[gdSyncFolderId] }
    override suspend fun setGoogleDriveSyncFolderId(id: String?) {
        dataStore.edit { it.updateOrRemove(gdSyncFolderId, id) }
    }

    override fun getGoogleDriveJournalsFolderId(): Flow<String?> = dataStore.data.map { it[gdJournalsFolderId] }
    override suspend fun setGoogleDriveJournalsFolderId(id: String?) {
        dataStore.edit { it.updateOrRemove(gdJournalsFolderId, id) }
    }

    override fun getGoogleDriveMediaFolderId(): Flow<String?> = dataStore.data.map { it[gdMediaFolderId] }
    override suspend fun setGoogleDriveMediaFolderId(id: String?) {
        dataStore.edit { it.updateOrRemove(gdMediaFolderId, id) }
    }

    override fun getSyncLoggingEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[syncLoggingEnabled] ?: false }

    override suspend fun setSyncLoggingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[syncLoggingEnabled] = enabled
        }
    }

    override fun getBackupLoggingEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[backupLoggingEnabled] ?: false }

    override suspend fun setBackupLoggingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[backupLoggingEnabled] = enabled
        }
    }

    override fun getDatabaseLoggingEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[databaseLoggingEnabled] ?: false }

    override suspend fun setDatabaseLoggingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[databaseLoggingEnabled] = enabled
        }
    }

    override fun isDeveloperModeEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[developerModeEnabled] ?: false }

    override suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[developerModeEnabled] = enabled
        }
    }

    override fun getLastCompletedDataRepairVersion(): Flow<Int> = dataStore.data.map { it[completedDataRepairVersion] ?: 0 }
    override suspend fun setLastCompletedDataRepairVersion(version: Int) {
        dataStore.edit { it[completedDataRepairVersion] = version }
    }

    private fun <T> MutablePreferences.updateOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value != null) this[key] = value else this.remove(key)
    }
}
