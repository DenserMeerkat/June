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

    override fun isAutomaticSyncEnabled(): Flow<Boolean> = dataStore.data.map { it[automaticSyncEnabled] ?: true }
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

    private fun <T> MutablePreferences.updateOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value != null) this[key] = value else this.remove(key)
    }
}
