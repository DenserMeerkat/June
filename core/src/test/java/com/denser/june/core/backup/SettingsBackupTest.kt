package com.denser.june.core.backup

import com.denser.june.core.data.backup.SettingsBackupRepoImpl
import com.denser.june.core.domain.backup.*
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.enums.*
import com.denser.june.core.domain.preferences.*
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek

class SettingsBackupTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `backup progress and summary models hold accurate metrics`() {
        val counts = PreBackupCounts(
            totalJournals = 42,
            totalMedia = 10,
            totalAudios = 5,
            totalSongs = 2,
            hasSettings = true,
            hasMapKeys = true
        )
        assertEquals(42, counts.totalJournals)
        assertEquals(10, counts.totalMedia)
        assertEquals(5, counts.totalAudios)
        assertEquals(2, counts.totalSongs)
        assertTrue(counts.hasSettings)
        assertTrue(counts.hasMapKeys)

        val progress = BackupOperationStatus.InProgress(
            phase = "Media",
            current = 5,
            total = 10,
            percentage = 0.5f,
            currentItemName = "photo.jpg"
        )
        assertEquals("Media", progress.phase)
        assertEquals(5, progress.current)
        assertEquals(10, progress.total)
        assertEquals(0.5f, progress.percentage, 0.001f)
        assertEquals("photo.jpg", progress.currentItemName)

        val summary = BackupSummary(
            totalJournals = 42,
            totalMedia = 10,
            totalAudios = 5,
            totalSongs = 2,
            hasSettings = true
        )
        assertEquals(42, summary.totalJournals)
        assertTrue(summary.hasSettings)
    }

    @Test
    fun `settings backup serialization roundtrip preserves all values`() {
        val original = SettingsBackup(
            version = 1,
            exportedAt = 123456789L,
            appearance = AppearanceSettingsBackup(
                themeMode = "DARK",
                seedColor = 0xFF112233.toInt(),
                amoled = true,
                paletteStyle = "Vibrant",
                materialTheme = false
            ),
            font = FontSettingsBackup(
                fontName = "Roboto",
                fontVariationConfigJson = null
            ),
            editor = EditorSettingsBackup(
                isMarkdownEnabled = true,
                alwaysOpenNewNote = true,
                keyboardAutocorrect = false,
                keyboardCapitalization = false,
                layoutDirection = "LTR",
                forceLtrUi = true
            ),
            general = GeneralSettingsBackup(
                autoTimeEnabled = true,
                startOfWeek = "MONDAY",
                timeFormat = "TWENTY_FOUR_HOUR",
                reminderEnabled = true,
                reminderTime = "20:30",
                reminderDays = listOf("MONDAY", "FRIDAY"),
                compressPhotosEnabled = true,
                photoCompressionQuality = 85,
                compressVideosEnabled = false,
                videoCompressionQuality = 720,
                compressAudioEnabled = true,
                audioCompressionQuality = 96
            ),
            map = MapSettingsBackup(
                mapTheme = "LIGHT",
                mapStyleProvider = "MAPTILER",
                mapThemeAccentEnabled = false,
                maptilerKey = "test_maptiler_key",
                stadiaKey = "test_stadia_key"
            ),
            privacy = PrivacySettingsBackup(
                screenPrivacy = true,
                isInternetAllowed = true
            )
        )

        val encoded = Json.encodeToString(SettingsBackup.serializer(), original)
        val decoded = json.decodeFromString<SettingsBackup>(encoded)

        assertEquals(original.version, decoded.version)
        assertEquals(original.exportedAt, decoded.exportedAt)
        assertEquals(original.appearance, decoded.appearance)
        assertEquals(original.font, decoded.font)
        assertEquals(original.editor, decoded.editor)
        assertEquals(original.general, decoded.general)
        assertEquals(original.map, decoded.map)
        assertEquals(original.privacy, decoded.privacy)
    }

    @Test
    fun `settings backup handles unknown fields gracefully`() {
        val jsonWithUnknownFields = """
            {
                "version": 2,
                "exportedAt": 999999,
                "unknownField": "should be ignored",
                "appearance": {
                    "themeMode": "LIGHT",
                    "seedColor": 123,
                    "amoled": false,
                    "paletteStyle": "TonalSpot",
                    "materialTheme": true,
                    "futureThemeColor": "neon"
                }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<SettingsBackup>(jsonWithUnknownFields)
        assertEquals(2, decoded.version)
        assertNotNull(decoded.appearance)
        assertEquals("LIGHT", decoded.appearance?.themeMode)
        assertTrue(decoded.appearance?.materialTheme == true)
    }

    @Test
    fun `exportSettingsJson excludes map keys when includeMapKeys is false`() = runTest {
        val fakeJournalPrefs = FakeJournalPreferences(
            maptilerKey = "maptiler_secret",
            stadiaKey = "stadia_secret"
        )
        val fakeThemePrefs = FakeThemePreferences()
        val fakeFontPrefs = FakeFontPreferences()
        val fakePrivacyPrefs = FakePrivacyPreferences()

        val repo = SettingsBackupRepoImpl(
            journalPreferences = fakeJournalPrefs,
            themePreferences = fakeThemePrefs,
            fontPreferences = fakeFontPrefs,
            privacyPreferences = fakePrivacyPrefs
        )

        val jsonString = repo.exportSettingsJson(includeMapKeys = false).getOrThrow()
        val backup = json.decodeFromString<SettingsBackup>(jsonString)

        assertNull("Maptiler key must be null when includeMapKeys = false", backup.map?.maptilerKey)
        assertNull("Stadia key must be null when includeMapKeys = false", backup.map?.stadiaKey)
        assertFalse(jsonString.contains("maptiler_secret"))
        assertFalse(jsonString.contains("stadia_secret"))
    }

    @Test
    fun `exportSettingsJson includes map keys when includeMapKeys is true`() = runTest {
        val fakeJournalPrefs = FakeJournalPreferences(
            maptilerKey = "maptiler_secret",
            stadiaKey = "stadia_secret"
        )
        val fakeThemePrefs = FakeThemePreferences()
        val fakeFontPrefs = FakeFontPreferences()
        val fakePrivacyPrefs = FakePrivacyPreferences()

        val repo = SettingsBackupRepoImpl(
            journalPreferences = fakeJournalPrefs,
            themePreferences = fakeThemePrefs,
            fontPreferences = fakeFontPrefs,
            privacyPreferences = fakePrivacyPrefs
        )

        val jsonString = repo.exportSettingsJson(includeMapKeys = true).getOrThrow()
        val backup = json.decodeFromString<SettingsBackup>(jsonString)

        assertEquals("maptiler_secret", backup.map?.maptilerKey)
        assertEquals("stadia_secret", backup.map?.stadiaKey)
        assertTrue(jsonString.contains("maptiler_secret"))
        assertTrue(jsonString.contains("stadia_secret"))
    }

    @Test
    fun `hasExportableMapKeys reflects whether any keys are configured`() = runTest {
        val fakeJournalPrefs = FakeJournalPreferences(maptilerKey = "", stadiaKey = "")
        val repo = SettingsBackupRepoImpl(
            journalPreferences = fakeJournalPrefs,
            themePreferences = FakeThemePreferences(),
            fontPreferences = FakeFontPreferences(),
            privacyPreferences = FakePrivacyPreferences()
        )

        assertFalse("Should be false when both keys are empty", repo.hasExportableMapKeys().first())

        fakeJournalPrefs.setMaptilerKey("my_maptiler_key")
        assertTrue("Should be true when maptiler key is set", repo.hasExportableMapKeys().first())

        fakeJournalPrefs.setMaptilerKey("")
        fakeJournalPrefs.setStadiaKey("my_stadia_key")
        assertTrue("Should be true when stadia key is set", repo.hasExportableMapKeys().first())
    }

    @Test
    fun `restoreSettingsJson updates preferences correctly`() = runTest {
        val fakeJournalPrefs = FakeJournalPreferences()
        val fakeThemePrefs = FakeThemePreferences()
        val fakeFontPrefs = FakeFontPreferences()
        val fakePrivacyPrefs = FakePrivacyPreferences()

        val repo = SettingsBackupRepoImpl(
            journalPreferences = fakeJournalPrefs,
            themePreferences = fakeThemePrefs,
            fontPreferences = fakeFontPrefs,
            privacyPreferences = fakePrivacyPrefs
        )

        val incomingBackup = SettingsBackup(
            appearance = AppearanceSettingsBackup(
                themeMode = "DARK",
                seedColor = 0x445566,
                amoled = true,
                paletteStyle = "FruitSalad",
                materialTheme = false
            ),
            font = FontSettingsBackup(
                fontName = "Custom Serif"
            ),
            editor = EditorSettingsBackup(
                isMarkdownEnabled = false,
                alwaysOpenNewNote = true,
                keyboardAutocorrect = false,
                keyboardCapitalization = false,
                layoutDirection = "RTL",
                forceLtrUi = false
            ),
            general = GeneralSettingsBackup(
                autoTimeEnabled = true,
                startOfWeek = "SATURDAY",
                timeFormat = "TWENTY_FOUR_HOUR",
                reminderEnabled = true,
                reminderTime = "18:00",
                reminderDays = listOf("SATURDAY", "SUNDAY"),
                compressPhotosEnabled = true,
                photoCompressionQuality = 90,
                compressVideosEnabled = true,
                videoCompressionQuality = 720,
                compressAudioEnabled = true,
                audioCompressionQuality = 64
            ),
            map = MapSettingsBackup(
                mapTheme = "DARK",
                mapStyleProvider = "STADIA",
                mapThemeAccentEnabled = true,
                maptilerKey = "restored_maptiler",
                stadiaKey = "restored_stadia"
            ),
            privacy = PrivacySettingsBackup(
                screenPrivacy = true,
                isInternetAllowed = false
            )
        )

        val jsonString = Json.encodeToString(SettingsBackup.serializer(), incomingBackup)
        val result = repo.restoreSettingsJson(jsonString)
        assertTrue(result.isSuccess)

        // Verify appearance
        assertEquals(ThemeMode.DARK, fakeThemePrefs.getThemeMode().first())
        assertEquals(0x445566, fakeThemePrefs.getSeedColorFlow().first())
        assertTrue(fakeThemePrefs.getAmoledPrefFlow().first())
        assertEquals(PaletteStyle.FruitSalad, fakeThemePrefs.getPaletteStyle().first())

        // Verify font
        assertEquals("Custom Serif", fakeFontPrefs.getAppFont().first())

        // Verify editor
        assertFalse(fakeJournalPrefs.isMarkdownEnabled().first())
        assertTrue(fakeJournalPrefs.alwaysOpenNewNote().first())
        assertEquals(EditorLayoutDirection.RTL, fakeJournalPrefs.editorLayoutDirection().first())

        // Verify general
        assertTrue(fakeJournalPrefs.isAutoTimeEnabled().first())
        assertEquals(DayOfWeek.SATURDAY, fakeJournalPrefs.startOfWeek().first())
        assertEquals(TimeFormat.TWENTY_FOUR_HOUR, fakeJournalPrefs.timeFormat().first())
        assertEquals("18:00", fakeJournalPrefs.reminderTime().first())
        assertTrue(fakeJournalPrefs.reminderDays().first().contains(DayOfWeek.SATURDAY))

        // Verify map
        assertEquals(MapStyleProvider.STADIA, fakeJournalPrefs.mapStyleProvider().first())
        assertEquals("restored_maptiler", fakeJournalPrefs.maptilerKey().first())
        assertEquals("restored_stadia", fakeJournalPrefs.stadiaKey().first())

        // Verify privacy
        assertTrue(fakePrivacyPrefs.getScreenPrivacyFlow().first())
        assertFalse(fakePrivacyPrefs.getIsInternetAllowedFlow().first())
    }

    // --- Test Fakes ---

    private class FakeJournalPreferences(
        maptilerKey: String = "",
        stadiaKey: String = ""
    ) : JournalPreferences {
        private val _autoTime = MutableStateFlow(false)
        private val _startOfWeek = MutableStateFlow(DayOfWeek.SUNDAY)
        private val _reminderEnabled = MutableStateFlow(false)
        private val _reminderTime = MutableStateFlow("21:14")
        private val _reminderDays = MutableStateFlow<Set<DayOfWeek>>(emptySet())
        private val _timeFormat = MutableStateFlow(TimeFormat.TWELVE_HOUR)
        private val _mapTheme = MutableStateFlow(MapTheme.APP)
        private val _isMarkdown = MutableStateFlow(true)
        private val _mapStyleProvider = MutableStateFlow(MapStyleProvider.CARTO)
        private val _maptilerKey = MutableStateFlow(maptilerKey)
        private val _stadiaKey = MutableStateFlow(stadiaKey)
        private val _alwaysOpenNote = MutableStateFlow(false)
        private val _autocorrect = MutableStateFlow(true)
        private val _capitalization = MutableStateFlow(true)
        private val _layoutDirection = MutableStateFlow(EditorLayoutDirection.AUTO)
        private val _forceLtr = MutableStateFlow(true)
        private val _compressPhotos = MutableStateFlow(false)
        private val _photoQuality = MutableStateFlow(80)
        private val _compressVideos = MutableStateFlow(false)
        private val _videoQuality = MutableStateFlow(1080)
        private val _compressAudio = MutableStateFlow(false)
        private val _audioQuality = MutableStateFlow(128)
        private val _mapAccent = MutableStateFlow(true)

        override fun isAutoTimeEnabled(): Flow<Boolean> = _autoTime
        override suspend fun setAutoTimeEnabled(enabled: Boolean) { _autoTime.value = enabled }
        override fun startOfWeek(): Flow<DayOfWeek> = _startOfWeek
        override suspend fun setStartOfWeek(dayOfWeek: DayOfWeek) { _startOfWeek.value = dayOfWeek }
        override fun isReminderEnabled(): Flow<Boolean> = _reminderEnabled
        override suspend fun setReminderEnabled(enabled: Boolean) { _reminderEnabled.value = enabled }
        override fun reminderTime(): Flow<String> = _reminderTime
        override suspend fun setReminderTime(time: String) { _reminderTime.value = time }
        override fun reminderDays(): Flow<Set<DayOfWeek>> = _reminderDays
        override suspend fun setReminderDays(days: Set<DayOfWeek>) { _reminderDays.value = days }
        override fun timeFormat(): Flow<TimeFormat> = _timeFormat
        override suspend fun setTimeFormat(format: TimeFormat) { _timeFormat.value = format }
        override fun mapTheme(): Flow<MapTheme> = _mapTheme
        override suspend fun setMapTheme(theme: MapTheme) { _mapTheme.value = theme }
        override fun isMarkdownEnabled(): Flow<Boolean> = _isMarkdown
        override suspend fun setMarkdownEnabled(enabled: Boolean) { _isMarkdown.value = enabled }
        override fun mapStyleProvider(): Flow<MapStyleProvider> = _mapStyleProvider
        override suspend fun setMapStyleProvider(provider: MapStyleProvider) { _mapStyleProvider.value = provider }
        override fun maptilerKey(): Flow<String> = _maptilerKey
        override suspend fun setMaptilerKey(key: String) { _maptilerKey.value = key }
        override fun stadiaKey(): Flow<String> = _stadiaKey
        override suspend fun setStadiaKey(key: String) { _stadiaKey.value = key }
        override fun isMapProviderVerified(provider: MapStyleProvider): Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setMapProviderVerified(provider: MapStyleProvider, verified: Boolean) {}
        override fun alwaysOpenNewNote(): Flow<Boolean> = _alwaysOpenNote
        override suspend fun setAlwaysOpenNewNote(enabled: Boolean) { _alwaysOpenNote.value = enabled }
        override fun isKeyboardAutocorrectEnabled(): Flow<Boolean> = _autocorrect
        override suspend fun setKeyboardAutocorrectEnabled(enabled: Boolean) { _autocorrect.value = enabled }
        override fun isKeyboardCapitalizationEnabled(): Flow<Boolean> = _capitalization
        override suspend fun setKeyboardCapitalizationEnabled(enabled: Boolean) { _capitalization.value = enabled }
        override fun editorLayoutDirection(): Flow<EditorLayoutDirection> = _layoutDirection
        override suspend fun setEditorLayoutDirection(direction: EditorLayoutDirection) { _layoutDirection.value = direction }
        override fun isForceLtrUiEnabled(): Flow<Boolean> = _forceLtr
        override suspend fun setForceLtrUiEnabled(enabled: Boolean) { _forceLtr.value = enabled }
        override fun isCompressPhotosEnabled(): Flow<Boolean> = _compressPhotos
        override suspend fun setCompressPhotosEnabled(enabled: Boolean) { _compressPhotos.value = enabled }
        override fun getPhotoCompressionQuality(): Flow<Int> = _photoQuality
        override suspend fun setPhotoCompressionQuality(quality: Int) { _photoQuality.value = quality }
        override fun isCompressVideosEnabled(): Flow<Boolean> = _compressVideos
        override suspend fun setCompressVideosEnabled(enabled: Boolean) { _compressVideos.value = enabled }
        override fun getVideoCompressionQuality(): Flow<Int> = _videoQuality
        override suspend fun setVideoCompressionQuality(quality: Int) { _videoQuality.value = quality }
        override fun isCompressAudioEnabled(): Flow<Boolean> = _compressAudio
        override suspend fun setCompressAudioEnabled(enabled: Boolean) { _compressAudio.value = enabled }
        override fun getAudioCompressionQuality(): Flow<Int> = _audioQuality
        override suspend fun setAudioCompressionQuality(quality: Int) { _audioQuality.value = quality }
        override fun isMapThemeAccentEnabled(): Flow<Boolean> = _mapAccent
        override suspend fun setMapThemeAccentEnabled(enabled: Boolean) { _mapAccent.value = enabled }
    }

    private class FakeThemePreferences : ThemePreferences {
        private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
        private val _seed = MutableStateFlow(0)
        private val _amoled = MutableStateFlow(false)
        private val _style = MutableStateFlow<PaletteStyle>(PaletteStyle.TonalSpot)
        private val _material = MutableStateFlow(false)

        override suspend fun resetAppTheme() {}
        override fun getThemeMode(): Flow<ThemeMode> = _mode
        override suspend fun updateThemeMode(pref: ThemeMode) { _mode.value = pref }
        override fun getSeedColorFlow(): Flow<Int> = _seed
        override suspend fun updateSeedColor(newCardContent: Int) { _seed.value = newCardContent }
        override fun getAmoledPrefFlow(): Flow<Boolean> = _amoled
        override suspend fun updateAmoledPref(amoled: Boolean) { _amoled.value = amoled }
        override fun getPaletteStyle(): Flow<PaletteStyle> = _style
        override suspend fun updatePaletteStyle(style: PaletteStyle) { _style.value = style }
        override fun getMaterialYouFlow(): Flow<Boolean> = _material
        override suspend fun updateMaterialTheme(pref: Boolean) { _material.value = pref }
    }

    private class FakeFontPreferences : FontPreferences {
        private val _font = MutableStateFlow("Google Sans Flex")
        private val _variation = MutableStateFlow<FontVariationConfig?>(null)

        override fun getAppFont(): Flow<String> = _font
        override suspend fun updateAppFont(fontName: String) { _font.value = fontName }
        override fun getAppFontVariation(): Flow<FontVariationConfig?> = _variation
        override suspend fun updateAppFontVariation(variation: FontVariationConfig?) { _variation.value = variation }
    }

    private class FakePrivacyPreferences : PrivacyPreferences {
        private val _screenPrivacy = MutableStateFlow(false)
        private val _internet = MutableStateFlow(true)

        override fun getAppLockFlow(): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun updateAppLock(enabled: Boolean) {}
        override suspend fun clearAppLockCredentialsAndDisableLock() {}
        override fun getLockTypeFlow(): Flow<LockType> = MutableStateFlow(LockType.BIOMETRIC)
        override suspend fun updateLockType(type: LockType) {}
        override fun getPinHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updatePinHash(hash: String?) {}
        override fun getPasswordHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updatePasswordHash(hash: String?) {}
        override fun getPatternHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updatePatternHash(hash: String?) {}
        override fun getSecurityQuestionFlow(): Flow<String?> = MutableStateFlow(null)
        override fun getSecurityAnswerHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updateSecurityQuestionAndAnswer(question: String?, answerHash: String?) {}
        override fun getVaultUseCustomLockFlow(): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun updateVaultUseCustomLock(enabled: Boolean) {}
        override fun getVaultLockTypeFlow(): Flow<LockType?> = MutableStateFlow(null)
        override suspend fun updateVaultLockType(type: LockType?) {}
        override suspend fun clearVaultCredentialsAndDisableCustomLock() {}
        override suspend fun setVaultCustomLock(type: LockType?, pinHash: String?, passwordHash: String?, patternHash: String?, securityQuestion: String?, securityAnswerHash: String?) {}
        override fun getVaultPinHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updateVaultPinHash(hash: String?) {}
        override fun getVaultPasswordHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updateVaultPasswordHash(hash: String?) {}
        override fun getVaultPatternHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updateVaultPatternHash(hash: String?) {}
        override fun getVaultSecurityQuestionFlow(): Flow<String?> = MutableStateFlow(null)
        override fun getVaultSecurityAnswerHashFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updateVaultSecurityQuestionAndAnswer(question: String?, answerHash: String?) {}
        override fun getScreenPrivacyFlow(): Flow<Boolean> = _screenPrivacy
        override suspend fun updateScreenPrivacy(enabled: Boolean) { _screenPrivacy.value = enabled }
        override fun getIsInternetAllowedFlow(): Flow<Boolean> = _internet
        override suspend fun updateIsInternetAllowed(allowed: Boolean) { _internet.value = allowed }
        override fun getLastChangelogShownFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updateLastChangelogShown(version: String) {}
        override fun getLastAnnouncementDismissedIdFlow(): Flow<String?> = MutableStateFlow(null)
        override suspend fun updateLastAnnouncementDismissedId(id: String) {}
        override fun getIsPlusPurchasedFlow(): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun updateIsPlusPurchased(isPurchased: Boolean) {}
    }
}

