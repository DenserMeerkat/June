package com.denser.june.core.backup

import com.denser.june.core.domain.backup.AutoBackupManager
import com.denser.june.core.domain.model.enums.AutoBackupInterval
import com.denser.june.core.domain.preferences.AutoBackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class FakeAutoBackupPreferences : AutoBackupPreferences {
    private val enabledFlow = MutableStateFlow(false)
    private val folderUriFlow = MutableStateFlow<String?>(null)
    private val intervalFlow = MutableStateFlow(AutoBackupInterval.WEEKLY)
    private val retentionFlow = MutableStateFlow(3)
    private val chargingFlow = MutableStateFlow(false)
    private val lastBackupFlow = MutableStateFlow(0L)
    private val includeMediaFlow = MutableStateFlow(true)
    private val includeAudiosFlow = MutableStateFlow(true)
    private val includeSongsFlow = MutableStateFlow(true)
    private val includeSettingsFlow = MutableStateFlow(true)

    override fun isAutoBackupEnabled(): Flow<Boolean> = enabledFlow
    override suspend fun setAutoBackupEnabled(enabled: Boolean) { enabledFlow.value = enabled }

    override fun getFolderUri(): Flow<String?> = folderUriFlow
    override suspend fun setFolderUri(uri: String?) { folderUriFlow.value = uri }

    override fun getInterval(): Flow<AutoBackupInterval> = intervalFlow
    override suspend fun setInterval(interval: AutoBackupInterval) { intervalFlow.value = interval }

    override fun getRetentionCount(): Flow<Int> = retentionFlow
    override suspend fun setRetentionCount(count: Int) { retentionFlow.value = count }

    override fun isRequireCharging(): Flow<Boolean> = chargingFlow
    override suspend fun setRequireCharging(require: Boolean) { chargingFlow.value = require }

    override fun getLastAutoBackupTime(): Flow<Long> = lastBackupFlow
    override suspend fun setLastAutoBackupTime(timestamp: Long) { lastBackupFlow.value = timestamp }

    override fun getIncludeMedia(): Flow<Boolean> = includeMediaFlow
    override suspend fun setIncludeMedia(include: Boolean) { includeMediaFlow.value = include }

    override fun getIncludeAudios(): Flow<Boolean> = includeAudiosFlow
    override suspend fun setIncludeAudios(include: Boolean) { includeAudiosFlow.value = include }

    override fun getIncludeSongs(): Flow<Boolean> = includeSongsFlow
    override suspend fun setIncludeSongs(include: Boolean) { includeSongsFlow.value = include }

    override fun getIncludeSettings(): Flow<Boolean> = includeSettingsFlow
    override suspend fun setIncludeSettings(include: Boolean) { includeSettingsFlow.value = include }
}

class AutoBackupTest {

    @Test
    fun `auto backup intervals calculate correct durations and parse names`() {
        assertEquals(1L, AutoBackupInterval.DAILY.days)
        assertEquals(TimeUnit.DAYS.toMillis(1), AutoBackupInterval.DAILY.repeatIntervalMillis)

        assertEquals(7L, AutoBackupInterval.WEEKLY.days)
        assertEquals(TimeUnit.DAYS.toMillis(7), AutoBackupInterval.WEEKLY.repeatIntervalMillis)

        assertEquals(30L, AutoBackupInterval.MONTHLY.days)
        assertEquals(TimeUnit.DAYS.toMillis(30), AutoBackupInterval.MONTHLY.repeatIntervalMillis)

        assertEquals(AutoBackupInterval.DAILY, AutoBackupInterval.fromName("DAILY"))
        assertEquals(AutoBackupInterval.DAILY, AutoBackupInterval.fromName("daily"))
        assertEquals(AutoBackupInterval.MONTHLY, AutoBackupInterval.fromName("monthly"))
        assertEquals(AutoBackupInterval.WEEKLY, AutoBackupInterval.fromName("UNKNOWN"))
        assertEquals(AutoBackupInterval.WEEKLY, AutoBackupInterval.fromName(null))
    }

    @Test
    fun `auto backup preferences store and retrieve properties correctly`() = runTest {
        val prefs = FakeAutoBackupPreferences()

        assertFalse(prefs.isAutoBackupEnabled().first())
        assertNull(prefs.getFolderUri().first())
        assertEquals(AutoBackupInterval.WEEKLY, prefs.getInterval().first())
        assertEquals(3, prefs.getRetentionCount().first())
        assertFalse(prefs.isRequireCharging().first())
        assertTrue(prefs.getIncludeMedia().first())
        assertTrue(prefs.getIncludeAudios().first())
        assertTrue(prefs.getIncludeSongs().first())
        assertTrue(prefs.getIncludeSettings().first())

        prefs.setAutoBackupEnabled(true)
        prefs.setFolderUri("content://com.android.externalstorage.documents/tree/primary%3ABackups")
        prefs.setInterval(AutoBackupInterval.DAILY)
        prefs.setRetentionCount(5)
        prefs.setRequireCharging(true)
        prefs.setIncludeMedia(false)
        prefs.setIncludeSongs(false)
        prefs.setLastAutoBackupTime(1700000000000L)

        assertTrue(prefs.isAutoBackupEnabled().first())
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3ABackups", prefs.getFolderUri().first())
        assertEquals(AutoBackupInterval.DAILY, prefs.getInterval().first())
        assertEquals(5, prefs.getRetentionCount().first())
        assertTrue(prefs.isRequireCharging().first())
        assertFalse(prefs.getIncludeMedia().first())
        assertTrue(prefs.getIncludeAudios().first())
        assertFalse(prefs.getIncludeSongs().first())
        assertTrue(prefs.getIncludeSettings().first())
        assertEquals(1700000000000L, prefs.getLastAutoBackupTime().first())
    }

    @Test
    fun `pruning algorithm correctly identifies and selects oldest files exceeding retention`() {
        val files = listOf(
            "June_Auto_Backup_20260101_100000.zip",
            "June_Auto_Backup_20260102_100000.zip",
            "June_Auto_Backup_20260103_100000.zip",
            "June_Auto_Backup_20260104_100000.zip",
            "June_Auto_Backup_20260105_100000.zip",
            "June_Manual_Backup_123.zip",
            "random_file.txt"
        )

        val autoBackups = files.filter {
            it.startsWith(AutoBackupManager.AUTO_BACKUP_PREFIX) && it.endsWith(".zip")
        }.sorted()

        assertEquals(5, autoBackups.size)

        val maxRetention = 3
        val toDelete = if (autoBackups.size > maxRetention) {
            autoBackups.take(autoBackups.size - maxRetention)
        } else emptyList()

        assertEquals(2, toDelete.size)
        assertEquals(listOf("June_Auto_Backup_20260101_100000.zip", "June_Auto_Backup_20260102_100000.zip"), toDelete)

        val remaining = autoBackups - toDelete.toSet()
        assertEquals(3, remaining.size)
        assertEquals(listOf(
            "June_Auto_Backup_20260103_100000.zip",
            "June_Auto_Backup_20260104_100000.zip",
            "June_Auto_Backup_20260105_100000.zip"
        ), remaining)
    }

    @Test
    fun `pruning with retention zero keeps all auto backups`() {
        val files = listOf(
            "June_Auto_Backup_20260101_100000.zip",
            "June_Auto_Backup_20260102_100000.zip"
        )
        val maxRetention = 0
        val toDelete = if (maxRetention > 0 && files.size > maxRetention) {
            files.take(files.size - maxRetention)
        } else emptyList()

        assertTrue(toDelete.isEmpty())
    }
}

