package com.denser.june.presentation.screens.settings

import android.net.Uri
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.enums.AutoBackupInterval
import com.denser.june.core.domain.model.enums.EditorLayoutDirection
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.domain.model.enums.FontType
import java.time.DayOfWeek
import com.materialkolor.PaletteStyle

sealed interface SettingsAction {
    data class OnSeedColorChange(val color: Int): SettingsAction
    data class OnThemeSwitch(val themeMode: ThemeMode): SettingsAction
    data class OnAmoledSwitch(val amoled: Boolean): SettingsAction
    data class OnPaletteChange(val style: PaletteStyle): SettingsAction
    data class OnMaterialThemeToggle(val pref: Boolean): SettingsAction
    data class OnPendingFontChange(val fontName: String): SettingsAction
    data class SetFontType(val type: FontType): SettingsAction
    data class OnFontSelect(val fontName: String): SettingsAction
    data class OnAppFontVariationChange(val variation: FontVariationConfig?): SettingsAction
    data object OnResetAppFontVariation: SettingsAction
    data object OnResetFonts: SettingsAction
    data object OnDeleteJournals: SettingsAction
    data object ResetBackup: SettingsAction
    data class OnRestoreJournals(val path: String): SettingsAction
    data class OnExportJournals(
        val includeMedia: Boolean = true,
        val includeAudios: Boolean = true,
        val includeSongFiles: Boolean = true,
        val includeSettings: Boolean = true,
        val includeMapKeys: Boolean = false
    ) : SettingsAction
    data object LoadPreBackupCounts : SettingsAction
    data class OnRestoreSettings(val json: String) : SettingsAction
    data object ResetSettingsRestore : SettingsAction
    data class OnExportMarkdown(
        val includeMedia: Boolean = true,
        val includeAudios: Boolean = true,
        val includeSongs: Boolean = true
    ) : SettingsAction
    data class OnImportMarkdownFiles(val uris: List<Uri>) : SettingsAction
    data class OnImportMarkdownZip(val uri: Uri) : SettingsAction
    data object ResetMarkdownImport : SettingsAction
    data class OnAppImport(val uri: Uri) : SettingsAction
    data object ResetAppImport : SettingsAction
    data class OnAppLockToggle(val enabled: Boolean) : SettingsAction
    data class UpdateLockType(val type: LockType) : SettingsAction
    data class UpdatePinHash(val hash: String?) : SettingsAction
    data class UpdatePasswordHash(val hash: String?) : SettingsAction
    data class UpdatePatternHash(val hash: String?) : SettingsAction
    data class UpdateSecurityQuestionAndAnswer(val question: String?, val answerHash: String?) : SettingsAction
    data class OnScreenPrivacyToggle(val enabled: Boolean) : SettingsAction
    data class OnAutoTimeToggle(val enabled: Boolean) : SettingsAction
    data class OnStartOfWeekChange(val startOfWeek: DayOfWeek) : SettingsAction
    data class OnTimeFormatChange(val timeFormat: TimeFormat) : SettingsAction
    data class OnMapThemeChange(val theme: MapTheme) : SettingsAction
    data class OnMarkdownToggle(val enabled: Boolean) : SettingsAction
    data class OnAlwaysOpenNewNoteToggle(val enabled: Boolean) : SettingsAction
    data class OnKeyboardAutocorrectToggle(val enabled: Boolean) : SettingsAction
    data class OnKeyboardCapitalizationToggle(val enabled: Boolean) : SettingsAction
    data class OnEditorLayoutDirectionChange(val direction: EditorLayoutDirection) : SettingsAction
    data class OnForceLtrUiToggle(val enabled: Boolean) : SettingsAction
    data class OnCompressPhotosToggle(val enabled: Boolean) : SettingsAction
    data class OnPhotoQualityChange(val quality: Int) : SettingsAction
    data class OnCompressVideosToggle(val enabled: Boolean) : SettingsAction
    data class OnVideoQualityChange(val quality: Int) : SettingsAction
    data class OnCompressAudioToggle(val enabled: Boolean) : SettingsAction
    data class OnAudioQualityChange(val quality: Int) : SettingsAction
    data class OnAutoBackupToggle(val enabled: Boolean) : SettingsAction
    data class OnAutoBackupFolderSelected(val uri: Uri) : SettingsAction
    data class OnAutoBackupIntervalChange(val interval: AutoBackupInterval) : SettingsAction
    data class OnAutoBackupRetentionChange(val count: Int) : SettingsAction
    data class OnAutoBackupRequireChargingToggle(val require: Boolean) : SettingsAction
    data class OnAutoBackupIncludeMediaToggle(val include: Boolean) : SettingsAction
    data class OnAutoBackupIncludeAudiosToggle(val include: Boolean) : SettingsAction
    data class OnAutoBackupIncludeSongsToggle(val include: Boolean) : SettingsAction
    data class OnAutoBackupIncludeSettingsToggle(val include: Boolean) : SettingsAction
    data object OnAutoBackupFolderCleared : SettingsAction
    data object OnTriggerAutoBackupNow : SettingsAction
}
