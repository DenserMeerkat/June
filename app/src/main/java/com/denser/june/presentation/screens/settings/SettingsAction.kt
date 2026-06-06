package com.denser.june.presentation.screens.settings

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
    data object OnResetFonts: SettingsAction
    data object OnDeleteJournals: SettingsAction
    data object ResetBackup: SettingsAction
    data class OnRestoreJournals(val path: String): SettingsAction
    data class OnExportJournals(val includeMedia: Boolean = true) : SettingsAction
    data class OnAppLockToggle(val enabled: Boolean) : SettingsAction
    data class UpdateLockType(val type: LockType) : SettingsAction
    data class UpdatePinHash(val hash: String?) : SettingsAction
    data class OnScreenPrivacyToggle(val enabled: Boolean) : SettingsAction
    data class OnAutoTimeToggle(val enabled: Boolean) : SettingsAction
    data class OnStartOfWeekChange(val startOfWeek: DayOfWeek) : SettingsAction
    data class OnTimeFormatChange(val timeFormat: TimeFormat) : SettingsAction
    data class OnMapThemeChange(val theme: MapTheme) : SettingsAction
    data class OnMarkdownToggle(val enabled: Boolean) : SettingsAction
}