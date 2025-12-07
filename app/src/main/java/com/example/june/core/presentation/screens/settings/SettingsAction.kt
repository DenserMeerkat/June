package com.example.june.core.presentation.screens.settings

import com.materialkolor.PaletteStyle

sealed interface SettingsAction {
    data class OnUpdateOnboardingDone(val done: Boolean) : SettingsAction
    data class OnSeedColorChange(val color: Int): SettingsAction
    data class OnThemeSwitch(val appTheme: com.example.june.core.domain.enums.AppTheme): SettingsAction
    data class OnAmoledSwitch(val amoled: Boolean): SettingsAction
    data class OnPaletteChange(val style: PaletteStyle): SettingsAction
    data class OnMaterialThemeToggle(val pref: Boolean): SettingsAction
    data class OnFontChange(val fonts: com.example.june.core.domain.enums.Fonts): SettingsAction
    data object OnDeleteNotes: SettingsAction
    data object ResetBackup: SettingsAction
    data class OnRestoreNotes(val path: String): SettingsAction
    data object OnExportNotes: SettingsAction
}