package com.denser.june.core.data.backup

import com.denser.june.core.domain.backup.*
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.enums.*
import com.denser.june.core.domain.preferences.*
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.DayOfWeek

class SettingsBackupRepoImpl(
    private val journalPreferences: JournalPreferences,
    private val themePreferences: ThemePreferences,
    private val fontPreferences: FontPreferences,
    private val privacyPreferences: PrivacyPreferences
) : SettingsBackupRepo {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val parserJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun hasExportableMapKeys(): Flow<Boolean> = combine(
        journalPreferences.maptilerKey(),
        journalPreferences.stadiaKey()
    ) { maptiler, stadia ->
        maptiler.isNotBlank() || stadia.isNotBlank()
    }

    override suspend fun exportSettingsJson(includeMapKeys: Boolean): Result<String> = runCatching {
        val themeMode = themePreferences.getThemeMode().first()
        val seedColor = themePreferences.getSeedColorFlow().first()
        val amoled = themePreferences.getAmoledPrefFlow().first()
        val paletteStyle = themePreferences.getPaletteStyle().first()
        val materialTheme = themePreferences.getMaterialYouFlow().first()

        val appearance = AppearanceSettingsBackup(
            themeMode = themeMode.name,
            seedColor = seedColor,
            amoled = amoled,
            paletteStyle = paletteStyle.name,
            materialTheme = materialTheme
        )

        val fontName = fontPreferences.getAppFont().first()
        val fontVariation = fontPreferences.getAppFontVariation().first()
        val fontVariationJson = fontVariation?.let {
            runCatching { Json.encodeToString(FontVariationConfig.serializer(), it) }.getOrNull()
        }

        val font = FontSettingsBackup(
            fontName = fontName,
            fontVariationConfigJson = fontVariationJson
        )

        val isMarkdown = journalPreferences.isMarkdownEnabled().first()
        val alwaysOpenNote = journalPreferences.alwaysOpenNewNote().first()
        val autocorrect = journalPreferences.isKeyboardAutocorrectEnabled().first()
        val capitalization = journalPreferences.isKeyboardCapitalizationEnabled().first()
        val layoutDirection = journalPreferences.editorLayoutDirection().first()
        val forceLtr = journalPreferences.isForceLtrUiEnabled().first()

        val editor = EditorSettingsBackup(
            isMarkdownEnabled = isMarkdown,
            alwaysOpenNewNote = alwaysOpenNote,
            keyboardAutocorrect = autocorrect,
            keyboardCapitalization = capitalization,
            layoutDirection = layoutDirection.name,
            forceLtrUi = forceLtr
        )

        val autoTime = journalPreferences.isAutoTimeEnabled().first()
        val startOfWeek = journalPreferences.startOfWeek().first()
        val timeFormat = journalPreferences.timeFormat().first()
        val reminderEnabled = journalPreferences.isReminderEnabled().first()
        val reminderTime = journalPreferences.reminderTime().first()
        val reminderDays = journalPreferences.reminderDays().first()
        val compressPhotos = journalPreferences.isCompressPhotosEnabled().first()
        val photoQuality = journalPreferences.getPhotoCompressionQuality().first()
        val compressVideos = journalPreferences.isCompressVideosEnabled().first()
        val videoQuality = journalPreferences.getVideoCompressionQuality().first()
        val compressAudio = journalPreferences.isCompressAudioEnabled().first()
        val audioQuality = journalPreferences.getAudioCompressionQuality().first()

        val general = GeneralSettingsBackup(
            autoTimeEnabled = autoTime,
            startOfWeek = startOfWeek.name,
            timeFormat = timeFormat.name,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime,
            reminderDays = reminderDays.map { it.name },
            compressPhotosEnabled = compressPhotos,
            photoCompressionQuality = photoQuality,
            compressVideosEnabled = compressVideos,
            videoCompressionQuality = videoQuality,
            compressAudioEnabled = compressAudio,
            audioCompressionQuality = audioQuality
        )

        val mapTheme = journalPreferences.mapTheme().first()
        val mapProvider = journalPreferences.mapStyleProvider().first()
        val mapAccent = journalPreferences.isMapThemeAccentEnabled().first()
        val maptilerKey = if (includeMapKeys) journalPreferences.maptilerKey().first().ifBlank { null } else null
        val stadiaKey = if (includeMapKeys) journalPreferences.stadiaKey().first().ifBlank { null } else null

        val map = MapSettingsBackup(
            mapTheme = mapTheme.name,
            mapStyleProvider = mapProvider.name,
            mapThemeAccentEnabled = mapAccent,
            maptilerKey = maptilerKey,
            stadiaKey = stadiaKey
        )

        val screenPrivacy = privacyPreferences.getScreenPrivacyFlow().first()
        val isInternetAllowed = privacyPreferences.getIsInternetAllowedFlow().first()

        val privacy = PrivacySettingsBackup(
            screenPrivacy = screenPrivacy,
            isInternetAllowed = isInternetAllowed
        )

        val backup = SettingsBackup(
            appearance = appearance,
            font = font,
            editor = editor,
            general = general,
            map = map,
            privacy = privacy
        )

        json.encodeToString(SettingsBackup.serializer(), backup)
    }

    override suspend fun restoreSettingsJson(json: String): Result<Unit> = runCatching {
        val backup = parserJson.decodeFromString<SettingsBackup>(json)

        backup.appearance?.let {
            runCatching { ThemeMode.valueOf(it.themeMode) }.getOrNull()?.let { mode ->
                themePreferences.updateThemeMode(mode)
            }
            if (it.seedColor != -1) {
                themePreferences.updateSeedColor(it.seedColor)
            }
            themePreferences.updateAmoledPref(it.amoled)
            runCatching { PaletteStyle.valueOf(it.paletteStyle) }.getOrNull()?.let { style ->
                themePreferences.updatePaletteStyle(style)
            }
            themePreferences.updateMaterialTheme(it.materialTheme)
        }

        backup.font?.let {
            if (it.fontName.isNotBlank()) {
                fontPreferences.updateAppFont(it.fontName)
            }
            it.fontVariationConfigJson?.let { configJson ->
                val variation = runCatching {
                    parserJson.decodeFromString<FontVariationConfig>(configJson)
                }.getOrNull()
                fontPreferences.updateAppFontVariation(variation)
            }
        }

        backup.editor?.let {
            journalPreferences.setMarkdownEnabled(it.isMarkdownEnabled)
            journalPreferences.setAlwaysOpenNewNote(it.alwaysOpenNewNote)
            journalPreferences.setKeyboardAutocorrectEnabled(it.keyboardAutocorrect)
            journalPreferences.setKeyboardCapitalizationEnabled(it.keyboardCapitalization)
            runCatching { EditorLayoutDirection.valueOf(it.layoutDirection) }.getOrNull()?.let { dir ->
                journalPreferences.setEditorLayoutDirection(dir)
            }
            journalPreferences.setForceLtrUiEnabled(it.forceLtrUi)
        }

        backup.general?.let {
            journalPreferences.setAutoTimeEnabled(it.autoTimeEnabled)
            runCatching { DayOfWeek.valueOf(it.startOfWeek) }.getOrNull()?.let { day ->
                journalPreferences.setStartOfWeek(day)
            }
            runCatching { TimeFormat.valueOf(it.timeFormat) }.getOrNull()?.let { format ->
                journalPreferences.setTimeFormat(format)
            }
            journalPreferences.setReminderEnabled(it.reminderEnabled)
            if (it.reminderTime.isNotBlank()) {
                journalPreferences.setReminderTime(it.reminderTime)
            }
            val days = it.reminderDays.mapNotNull { d -> runCatching { DayOfWeek.valueOf(d) }.getOrNull() }.toSet()
            if (days.isNotEmpty()) {
                journalPreferences.setReminderDays(days)
            }
            journalPreferences.setCompressPhotosEnabled(it.compressPhotosEnabled)
            journalPreferences.setPhotoCompressionQuality(it.photoCompressionQuality)
            journalPreferences.setCompressVideosEnabled(it.compressVideosEnabled)
            journalPreferences.setVideoCompressionQuality(it.videoCompressionQuality)
            journalPreferences.setCompressAudioEnabled(it.compressAudioEnabled)
            journalPreferences.setAudioCompressionQuality(it.audioCompressionQuality)
        }

        backup.map?.let {
            runCatching { MapTheme.valueOf(it.mapTheme) }.getOrNull()?.let { theme ->
                journalPreferences.setMapTheme(theme)
            }
            runCatching { MapStyleProvider.valueOf(it.mapStyleProvider) }.getOrNull()?.let { provider ->
                journalPreferences.setMapStyleProvider(provider)
            }
            journalPreferences.setMapThemeAccentEnabled(it.mapThemeAccentEnabled)
            if (it.maptilerKey != null) {
                journalPreferences.setMaptilerKey(it.maptilerKey)
            }
            if (it.stadiaKey != null) {
                journalPreferences.setStadiaKey(it.stadiaKey)
            }
        }

        backup.privacy?.let {
            privacyPreferences.updateScreenPrivacy(it.screenPrivacy)
            privacyPreferences.updateIsInternetAllowed(it.isInternetAllowed)
        }
    }
}

