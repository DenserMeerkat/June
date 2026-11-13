package com.denser.june.core.domain.backup

import kotlinx.serialization.Serializable

@Serializable
data class SettingsBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appearance: AppearanceSettingsBackup? = null,
    val font: FontSettingsBackup? = null,
    val editor: EditorSettingsBackup? = null,
    val general: GeneralSettingsBackup? = null,
    val map: MapSettingsBackup? = null,
    val privacy: PrivacySettingsBackup? = null
)

@Serializable
data class AppearanceSettingsBackup(
    val themeMode: String = "SYSTEM",
    val seedColor: Int = -1,
    val amoled: Boolean = false,
    val paletteStyle: String = "TonalSpot",
    val materialTheme: Boolean = false
)

@Serializable
data class FontSettingsBackup(
    val fontName: String = "App",
    val fontVariationConfigJson: String? = null
)

@Serializable
data class EditorSettingsBackup(
    val isMarkdownEnabled: Boolean = true,
    val alwaysOpenNewNote: Boolean = false,
    val keyboardAutocorrect: Boolean = true,
    val keyboardCapitalization: Boolean = true,
    val layoutDirection: String = "AUTO",
    val forceLtrUi: Boolean = true
)

@Serializable
data class GeneralSettingsBackup(
    val autoTimeEnabled: Boolean = false,
    val startOfWeek: String = "SUNDAY",
    val timeFormat: String = "TWELVE_HOUR",
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "21:14",
    val reminderDays: List<String> = emptyList(),
    val compressPhotosEnabled: Boolean = false,
    val photoCompressionQuality: Int = 80,
    val compressVideosEnabled: Boolean = false,
    val videoCompressionQuality: Int = 1080,
    val compressAudioEnabled: Boolean = false,
    val audioCompressionQuality: Int = 128
)

@Serializable
data class MapSettingsBackup(
    val mapTheme: String = "APP",
    val mapStyleProvider: String = "CARTO",
    val mapThemeAccentEnabled: Boolean = true,
    val maptilerKey: String? = null,
    val stadiaKey: String? = null
)

@Serializable
data class PrivacySettingsBackup(
    val screenPrivacy: Boolean = false,
    val isInternetAllowed: Boolean = true
)

