package com.denser.june.core.domain.preferences

import com.denser.june.core.domain.model.enums.LockType
import kotlinx.coroutines.flow.Flow

interface PrivacyPreferences {
    fun getAppLockFlow(): Flow<Boolean>
    suspend fun updateAppLock(enabled: Boolean)

    fun getLockTypeFlow(): Flow<LockType>
    suspend fun updateLockType(type: LockType)

    fun getPinHashFlow(): Flow<String?>
    suspend fun updatePinHash(hash: String?)

    fun getScreenPrivacyFlow(): Flow<Boolean>
    suspend fun updateScreenPrivacy(enabled: Boolean)
}
