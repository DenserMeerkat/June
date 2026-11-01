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
        private val passwordHash = stringPreferencesKey("password_hash")
        private val patternHash = stringPreferencesKey("pattern_hash")
        private val securityQuestion = stringPreferencesKey("security_question")
        private val securityAnswerHash = stringPreferencesKey("security_answer_hash")
        private val vaultUseCustomLock = booleanPreferencesKey("vault_use_custom_lock")
        private val vaultLockType = stringPreferencesKey("vault_lock_type")
        private val vaultPinHash = stringPreferencesKey("vault_pin_hash")
        private val vaultPasswordHash = stringPreferencesKey("vault_password_hash")
        private val vaultPatternHash = stringPreferencesKey("vault_pattern_hash")
        private val vaultSecurityQuestion = stringPreferencesKey("vault_security_question")
        private val vaultSecurityAnswerHash = stringPreferencesKey("vault_security_answer_hash")
        private val screenPrivacy = booleanPreferencesKey("screen_privacy")
        private val isInternetAllowed = booleanPreferencesKey("is_internet_allowed")
        private val lastChangelogShown = stringPreferencesKey("last_changelog_shown")
        private val lastAnnouncementDismissedId = stringPreferencesKey("last_announcement_dismissed_id")
    }

    override fun getAppLockFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[appLock] == true }

    override suspend fun updateAppLock(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[appLock] = enabled
        }
    }

    override suspend fun clearAppLockCredentialsAndDisableLock() {
        dataStore.edit {
            it[appLock] = false
            it.remove(pinHash)
            it.remove(passwordHash)
            it.remove(patternHash)
            it.remove(securityQuestion)
            it.remove(securityAnswerHash)
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

    override fun getPasswordHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[passwordHash] }

    override suspend fun updatePasswordHash(hash: String?) {
        dataStore.edit {
            if (hash == null) it.remove(passwordHash) else it[passwordHash] = hash
        }
    }

    override fun getPatternHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[patternHash] }

    override suspend fun updatePatternHash(hash: String?) {
        dataStore.edit {
            if (hash == null) it.remove(patternHash) else it[patternHash] = hash
        }
    }

    override fun getSecurityQuestionFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[securityQuestion] }

    override fun getSecurityAnswerHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[securityAnswerHash] }

    override suspend fun updateSecurityQuestionAndAnswer(question: String?, answerHash: String?) {
        dataStore.edit { preferences ->
            if (question == null) {
                preferences.remove(securityQuestion)
            } else {
                preferences[securityQuestion] = question
            }
            if (answerHash == null) {
                preferences.remove(securityAnswerHash)
            } else {
                preferences[securityAnswerHash] = answerHash
            }
        }
    }

    override fun getVaultUseCustomLockFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[vaultUseCustomLock] == true }

    override suspend fun updateVaultUseCustomLock(enabled: Boolean) {
        dataStore.edit { it[vaultUseCustomLock] = enabled }
    }

    override fun getVaultLockTypeFlow(): Flow<LockType?> = dataStore.data
        .map { preferences ->
            val typeName = preferences[vaultLockType]
            if (typeName != null) {
                try {
                    LockType.valueOf(typeName)
                } catch (e: Exception) {
                    null
                }
            } else null
        }

    override suspend fun updateVaultLockType(type: LockType?) {
        dataStore.edit {
            if (type == null) it.remove(vaultLockType) else it[vaultLockType] = type.name
        }
    }

    override suspend fun clearVaultCredentialsAndDisableCustomLock() {
        dataStore.edit {
            it[vaultUseCustomLock] = false
            it.remove(vaultLockType)
            it.remove(vaultPinHash)
            it.remove(vaultPasswordHash)
            it.remove(vaultPatternHash)
            it.remove(vaultSecurityQuestion)
            it.remove(vaultSecurityAnswerHash)
        }
    }

    override suspend fun setVaultCustomLock(
        type: LockType?,
        pinHash: String?,
        passwordHash: String?,
        patternHash: String?,
        securityQuestion: String?,
        securityAnswerHash: String?
    ) {
        dataStore.edit {
            it[vaultUseCustomLock] = true
            if (type == null) it.remove(vaultLockType) else it[vaultLockType] = type.name
            if (pinHash == null) it.remove(vaultPinHash) else it[vaultPinHash] = pinHash
            if (passwordHash == null) it.remove(vaultPasswordHash) else it[vaultPasswordHash] = passwordHash
            if (patternHash == null) it.remove(vaultPatternHash) else it[vaultPatternHash] = patternHash
            if (securityQuestion == null) it.remove(vaultSecurityQuestion) else it[vaultSecurityQuestion] = securityQuestion
            if (securityAnswerHash == null) it.remove(vaultSecurityAnswerHash) else it[vaultSecurityAnswerHash] = securityAnswerHash
        }
    }

    override fun getVaultPinHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[vaultPinHash] }

    override suspend fun updateVaultPinHash(hash: String?) {
        dataStore.edit {
            if (hash == null) it.remove(vaultPinHash) else it[vaultPinHash] = hash
        }
    }

    override fun getVaultPasswordHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[vaultPasswordHash] }

    override suspend fun updateVaultPasswordHash(hash: String?) {
        dataStore.edit {
            if (hash == null) it.remove(vaultPasswordHash) else it[vaultPasswordHash] = hash
        }
    }

    override fun getVaultPatternHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[vaultPatternHash] }

    override suspend fun updateVaultPatternHash(hash: String?) {
        dataStore.edit {
            if (hash == null) it.remove(vaultPatternHash) else it[vaultPatternHash] = hash
        }
    }

    override fun getVaultSecurityQuestionFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[vaultSecurityQuestion] }

    override fun getVaultSecurityAnswerHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[vaultSecurityAnswerHash] }

    override suspend fun updateVaultSecurityQuestionAndAnswer(question: String?, answerHash: String?) {
        dataStore.edit { preferences ->
            if (question == null) {
                preferences.remove(vaultSecurityQuestion)
            } else {
                preferences[vaultSecurityQuestion] = question
            }
            if (answerHash == null) {
                preferences.remove(vaultSecurityAnswerHash)
            } else {
                preferences[vaultSecurityAnswerHash] = answerHash
            }
        }
    }

    override fun getScreenPrivacyFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[screenPrivacy] == true }

    override suspend fun updateScreenPrivacy(enabled: Boolean) {
        dataStore.edit { it[screenPrivacy] = enabled }
    }

    override fun getIsInternetAllowedFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[isInternetAllowed] ?: true }

    override suspend fun updateIsInternetAllowed(allowed: Boolean) {
        dataStore.edit { it[isInternetAllowed] = allowed }
    }

    override fun getLastChangelogShownFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[lastChangelogShown] }

    override suspend fun updateLastChangelogShown(version: String) {
        dataStore.edit { it[lastChangelogShown] = version }
    }

    override fun getLastAnnouncementDismissedIdFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[lastAnnouncementDismissedId] }

    override suspend fun updateLastAnnouncementDismissedId(id: String) {
        dataStore.edit { it[lastAnnouncementDismissedId] = id }
    }
}
