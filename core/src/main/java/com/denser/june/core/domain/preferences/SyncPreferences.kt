package com.denser.june.core.domain.preferences

import kotlinx.coroutines.flow.Flow

interface SyncPreferences {
    fun getLastSyncTime(): Flow<Long>
    suspend fun setLastSyncTime(time: Long)

    fun getSyncEnabled(): Flow<Boolean>
    suspend fun setSyncEnabled(enabled: Boolean)

    fun getSelectedProvider(): Flow<String?>
    suspend fun setSelectedProvider(providerName: String?)

    fun getSyncOnlyOnWifi(): Flow<Boolean>
    suspend fun setSyncOnlyOnWifi(onlyWifi: Boolean)

    fun getWebDavUrl(): Flow<String?>
    suspend fun setWebDavUrl(url: String?)

    fun getWebDavUsername(): Flow<String?>
    suspend fun setWebDavUsername(username: String?)

    fun getWebDavPassword(): Flow<String?>
    suspend fun setWebDavPassword(password: String?)

    fun isAutomaticSyncEnabled(): Flow<Boolean>
    suspend fun setAutomaticSyncEnabled(enabled: Boolean)

    suspend fun getDeviceId(): String

    fun getGoogleDriveSyncFolderId(): Flow<String?>
    suspend fun setGoogleDriveSyncFolderId(id: String?)

    fun getGoogleDriveJournalsFolderId(): Flow<String?>
    suspend fun setGoogleDriveJournalsFolderId(id: String?)

    fun getGoogleDriveMediaFolderId(): Flow<String?>
    suspend fun setGoogleDriveMediaFolderId(id: String?)

    fun getLastCompletedDataRepairVersion(): Flow<Int>
    suspend fun setLastCompletedDataRepairVersion(version: Int)
}
