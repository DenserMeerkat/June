package com.denser.june.core.domain.preferences

import com.denser.june.core.domain.model.enums.LockType
import kotlinx.coroutines.flow.Flow

interface PrivacyPreferences {
    fun getAppLockFlow(): Flow<Boolean>
    suspend fun updateAppLock(enabled: Boolean)
    suspend fun clearAppLockCredentialsAndDisableLock()

    fun getLockTypeFlow(): Flow<LockType>
    suspend fun updateLockType(type: LockType)

    fun getPinHashFlow(): Flow<String?>
    suspend fun updatePinHash(hash: String?)

    fun getPasswordHashFlow(): Flow<String?>
    suspend fun updatePasswordHash(hash: String?)

    fun getPatternHashFlow(): Flow<String?>
    suspend fun updatePatternHash(hash: String?)

    fun getSecurityQuestionFlow(): Flow<String?>
    fun getSecurityAnswerHashFlow(): Flow<String?>
    suspend fun updateSecurityQuestionAndAnswer(question: String?, answerHash: String?)

    fun getVaultUseCustomLockFlow(): Flow<Boolean>
    suspend fun updateVaultUseCustomLock(enabled: Boolean)

    fun getVaultLockTypeFlow(): Flow<LockType?>
    suspend fun updateVaultLockType(type: LockType?)

    suspend fun clearVaultCredentialsAndDisableCustomLock()
    suspend fun setVaultCustomLock(
        type: LockType?,
        pinHash: String? = null,
        passwordHash: String? = null,
        patternHash: String? = null,
        securityQuestion: String? = null,
        securityAnswerHash: String? = null
    )

    fun getVaultPinHashFlow(): Flow<String?>
    suspend fun updateVaultPinHash(hash: String?)

    fun getVaultPasswordHashFlow(): Flow<String?>
    suspend fun updateVaultPasswordHash(hash: String?)

    fun getVaultPatternHashFlow(): Flow<String?>
    suspend fun updateVaultPatternHash(hash: String?)

    fun getVaultSecurityQuestionFlow(): Flow<String?>
    fun getVaultSecurityAnswerHashFlow(): Flow<String?>
    suspend fun updateVaultSecurityQuestionAndAnswer(question: String?, answerHash: String?)

    fun getScreenPrivacyFlow(): Flow<Boolean>
    suspend fun updateScreenPrivacy(enabled: Boolean)

    fun getIsInternetAllowedFlow(): Flow<Boolean>
    suspend fun updateIsInternetAllowed(allowed: Boolean)

    fun getLastChangelogShownFlow(): Flow<String?>
    suspend fun updateLastChangelogShown(version: String)

    fun getLastAnnouncementDismissedIdFlow(): Flow<String?>
    suspend fun updateLastAnnouncementDismissedId(id: String)

    fun getIsPlusPurchasedFlow(): Flow<Boolean>
    suspend fun updateIsPlusPurchased(isPurchased: Boolean)
}
