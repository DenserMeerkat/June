package com.denser.june.core.sync.fakes

import com.denser.june.core.domain.preferences.SyncPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory FakeSyncPreferences for unit testing.
 */
class FakeSyncPreferences(
    syncEnabled: Boolean = true,
    selectedProvider: String? = "FakeCloud",
    deviceId: String = "test-device-A"
) : SyncPreferences {

    var syncEnabledValue = syncEnabled
    var autoSyncEnabled = false
    var syncOnlyOnWifi = false
    var webDavUrl: String? = null
    var webDavUsername: String? = null
    var webDavPassword: String? = null
    var lastSyncTime = 0L
    var selectedProviderValue: String? = selectedProvider
    var deviceIdValue = deviceId
    var syncLoggingEnabled = false
    var backupLoggingEnabled = false
    var databaseLoggingEnabled = false
    var developerMode = false
    var lastCompletedDataRepairVersion = 0
    var googleDriveSyncFolderId: String? = null
    var googleDriveJournalsFolderId: String? = null
    var googleDriveMediaFolderId: String? = null

    private val _syncEnabled = MutableStateFlow(syncEnabled)
    private val _autoSync = MutableStateFlow(false)
    private val _wifiOnly = MutableStateFlow(false)
    private val _lastSyncTime = MutableStateFlow(0L)
    private val _selectedProvider = MutableStateFlow<String?>(selectedProvider)
    private val _webDavUrl = MutableStateFlow<String?>(null)
    private val _webDavUser = MutableStateFlow<String?>(null)
    private val _webDavPass = MutableStateFlow<String?>(null)
    private val _syncLogging = MutableStateFlow(false)
    private val _backupLogging = MutableStateFlow(false)
    private val _dbLogging = MutableStateFlow(false)
    private val _devMode = MutableStateFlow(false)
    private val _dataRepairVersion = MutableStateFlow(0)
    private val _gdriveSyncFolder = MutableStateFlow<String?>(null)
    private val _gdriveJournalsFolder = MutableStateFlow<String?>(null)
    private val _gdriveMediaFolder = MutableStateFlow<String?>(null)

    override fun getLastSyncTime(): Flow<Long> = _lastSyncTime
    override suspend fun setLastSyncTime(time: Long) { lastSyncTime = time; _lastSyncTime.value = time }
    override fun getSyncEnabled(): Flow<Boolean> = _syncEnabled
    override suspend fun setSyncEnabled(enabled: Boolean) { syncEnabledValue = enabled; _syncEnabled.value = enabled }
    override fun getSelectedProvider(): Flow<String?> = _selectedProvider
    override suspend fun setSelectedProvider(providerName: String?) { selectedProviderValue = providerName; _selectedProvider.value = providerName }
    override fun getSyncOnlyOnWifi(): Flow<Boolean> = _wifiOnly
    override suspend fun setSyncOnlyOnWifi(onlyWifi: Boolean) { syncOnlyOnWifi = onlyWifi; _wifiOnly.value = onlyWifi }
    override fun getWebDavUrl(): Flow<String?> = _webDavUrl
    override suspend fun setWebDavUrl(url: String?) { webDavUrl = url; _webDavUrl.value = url }
    override fun getWebDavUsername(): Flow<String?> = _webDavUser
    override suspend fun setWebDavUsername(username: String?) { webDavUsername = username; _webDavUser.value = username }
    override fun getWebDavPassword(): Flow<String?> = _webDavPass
    override suspend fun setWebDavPassword(password: String?) { webDavPassword = password; _webDavPass.value = password }
    override fun isAutomaticSyncEnabled(): Flow<Boolean> = _autoSync
    override suspend fun setAutomaticSyncEnabled(enabled: Boolean) { autoSyncEnabled = enabled; _autoSync.value = enabled }
    override suspend fun getDeviceId(): String = deviceIdValue
    override fun getGoogleDriveSyncFolderId(): Flow<String?> = _gdriveSyncFolder
    override suspend fun setGoogleDriveSyncFolderId(id: String?) { googleDriveSyncFolderId = id; _gdriveSyncFolder.value = id }
    override fun getGoogleDriveJournalsFolderId(): Flow<String?> = _gdriveJournalsFolder
    override suspend fun setGoogleDriveJournalsFolderId(id: String?) { googleDriveJournalsFolderId = id; _gdriveJournalsFolder.value = id }
    override fun getGoogleDriveMediaFolderId(): Flow<String?> = _gdriveMediaFolder
    override suspend fun setGoogleDriveMediaFolderId(id: String?) { googleDriveMediaFolderId = id; _gdriveMediaFolder.value = id }
    override fun getSyncLoggingEnabled(): Flow<Boolean> = _syncLogging
    override suspend fun setSyncLoggingEnabled(enabled: Boolean) { syncLoggingEnabled = enabled; _syncLogging.value = enabled }
    override fun getBackupLoggingEnabled(): Flow<Boolean> = _backupLogging
    override suspend fun setBackupLoggingEnabled(enabled: Boolean) { backupLoggingEnabled = enabled; _backupLogging.value = enabled }
    override fun getDatabaseLoggingEnabled(): Flow<Boolean> = _dbLogging
    override suspend fun setDatabaseLoggingEnabled(enabled: Boolean) { databaseLoggingEnabled = enabled; _dbLogging.value = enabled }
    override fun isDeveloperModeEnabled(): Flow<Boolean> = _devMode
    override suspend fun setDeveloperModeEnabled(enabled: Boolean) { developerMode = enabled; _devMode.value = enabled }
    override fun getLastCompletedDataRepairVersion(): Flow<Int> = _dataRepairVersion
    override suspend fun setLastCompletedDataRepairVersion(version: Int) { lastCompletedDataRepairVersion = version; _dataRepairVersion.value = version }
}
