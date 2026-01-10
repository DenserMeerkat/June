package com.denser.june.core.presentation.screens.settings

import com.denser.june.core.domain.enums.AppTheme
import com.denser.june.core.domain.enums.Fonts
import com.materialkolor.PaletteStyle

sealed interface SettingsAction {
    data class OnUpdateOnboardingDone(val done: Boolean) : SettingsAction
    data class OnSeedColorChange(val color: Int): SettingsAction
    data class OnThemeSwitch(val appTheme: AppTheme): SettingsAction
    data class OnAmoledSwitch(val amoled: Boolean): SettingsAction
    data class OnPaletteChange(val style: PaletteStyle): SettingsAction
    data class OnMaterialThemeToggle(val pref: Boolean): SettingsAction
    data class OnFontChange(val fonts: Fonts): SettingsAction
    data object OnDeleteJournals: SettingsAction
    data object ResetBackup: SettingsAction
    data class OnRestoreJournals(val path: String): SettingsAction
    data class OnExportJournals(val includeMedia: Boolean = true) : SettingsAction
    data class OnAppLockToggle(val enabled: Boolean) : SettingsAction
}