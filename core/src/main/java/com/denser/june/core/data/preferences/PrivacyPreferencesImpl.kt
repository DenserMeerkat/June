package com.denser.june.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.model.enums.LockType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrivacyPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : PrivacyPreferences {

    companion object {
        private val appLock = booleanPreferencesKey("app_lock")
        private val lockType = stringPreferencesKey("lock_type")
        private val pinHash = stringPreferencesKey("pin_hash")
        private val screenPrivacy = booleanPreferencesKey("screen_privacy")
    }

    override fun getAppLockFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[appLock] == true }

    override suspend fun updateAppLock(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[appLock] = enabled
        }
    }

    override fun getLockTypeFlow(): Flow<LockType> = dataStore.data
        .map { preferences ->
            val typeName = preferences[lockType] ?: LockType.BIOMETRIC.name
            LockType.valueOf(typeName)
        }

    override suspend fun updateLockType(type: LockType) {
        dataStore.edit { it[lockType] = type.name }
    }

    override fun getPinHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[pinHash] }

    override suspend fun updatePinHash(hash: String?) {
        dataStore.edit {
            if (hash == null) it.remove(pinHash) else it[pinHash] = hash
        }
    }

    override fun getScreenPrivacyFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[screenPrivacy] == true }

    override suspend fun updateScreenPrivacy(enabled: Boolean) {
        dataStore.edit { it[screenPrivacy] = enabled }
    }
}
