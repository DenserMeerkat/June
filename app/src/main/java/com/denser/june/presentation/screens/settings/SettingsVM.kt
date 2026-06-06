package com.denser.june.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.domain.preferences.FontPreferences
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.ExportState
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.domain.backup.RestoreResult
import com.denser.june.core.domain.backup.RestoreState
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.domain.model.enums.FontType
import java.time.DayOfWeek
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsVM(
    private val repo: JournalRepository,
    private val themePrefs: ThemePreferences,
    private val privacyPrefs: PrivacyPreferences,
    private val journalPrefs: JournalPreferences,
    private val fontPrefs: FontPreferences,
    private val exportRepo: ExportRepo,
    private val restoreRepo: RestoreRepo
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsState())

    val state = combine<Any?, SettingsState>(
        listOf(
            _localState,
            themePrefs.getSeedColorFlow(),
            themePrefs.getThemeMode(),
            themePrefs.getAmoledPrefFlow(),
            themePrefs.getPaletteStyle(),
            themePrefs.getMaterialYouFlow(),
            privacyPrefs.getAppLockFlow(),
            privacyPrefs.getLockTypeFlow(),
            privacyPrefs.getPinHashFlow(),
            privacyPrefs.getScreenPrivacyFlow(),
            journalPrefs.isAutoTimeEnabled(),
            journalPrefs.startOfWeek(),
            journalPrefs.timeFormat(),
            fontPrefs.getAppFont(),
            privacyPrefs.getIsInternetAllowedFlow(),
            journalPrefs.mapTheme(),
            journalPrefs.isMarkdownEnabled()
        )
    ) { array ->
        val local = array[0] as SettingsState

        local.copy(
            isAppLockEnabled = array[6] as Boolean,
            lockType = array[7] as LockType,
            pinHash = array[8] as String?,
            isScreenPrivacyEnabled = array[9] as Boolean,
            isAutoTimeEnabled = array[10] as Boolean,
            startOfWeek = array[11] as DayOfWeek,
            timeFormat = array[12] as TimeFormat,
            isInternetAllowed = array[14] as Boolean,
            mapTheme = array[15] as MapTheme,
            isMarkdownEnabled = array[16] as Boolean,

            appTheme = local.appTheme.copy(
                seedColor = array[1] as Int,
                themeMode = array[2] as ThemeMode,
                withAmoled = array[3] as Boolean,
                style = array[4] as PaletteStyle,
                materialTheme = array[5] as Boolean,
                appFont = array[13] as String,
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                SettingsAction.OnDeleteJournals -> {
                    withContext(Dispatchers.IO) {
                        repo.deleteAllJournals()
                    }
                }

                is SettingsAction.OnExportJournals -> {
                    _localState.update { it.copy(exportState = ExportState.Exporting) }
                    val result = exportRepo.exportData(includeMedia = action.includeMedia)
                    _localState.update {
                        it.copy(exportState = if (result != null) ExportState.ExportReady(result) else ExportState.Error)
                    }
                }

                is SettingsAction.OnRestoreJournals -> {
                    _localState.update { it.copy(restoreState = RestoreState.Restoring) }
                    when (val res = restoreRepo.restoreData(action.path)) {
                        is RestoreResult.Failure -> _localState.update {
                            it.copy(
                                restoreState = RestoreState.Failure(
                                    res.exceptionType
                                )
                            )
                        }

                        RestoreResult.Success -> _localState.update { it.copy(restoreState = RestoreState.Restored) }
                    }
                }
                SettingsAction.ResetBackup -> _localState.update {
                    it.copy(
                        restoreState = RestoreState.Idle,
                        exportState = ExportState.Idle
                    )
                }
                is SettingsAction.OnSeedColorChange -> themePrefs.updateSeedColor(action.color)
                is SettingsAction.OnAmoledSwitch -> themePrefs.updateAmoledPref(action.amoled)
                is SettingsAction.OnThemeSwitch -> themePrefs.updateThemeMode(action.themeMode)
                is SettingsAction.OnPaletteChange -> themePrefs.updatePaletteStyle(action.style)
                is SettingsAction.OnPendingFontChange -> _localState.update { it.copy(pendingFontName = action.fontName) }
                is SettingsAction.SetFontType -> _localState.update { it.copy(selectedFontType = action.type, pendingFontName = null) }
                is SettingsAction.OnFontSelect -> {
                    when (_localState.value.selectedFontType) {
                        FontType.APP -> fontPrefs.updateAppFont(action.fontName)
                    }
                }
                SettingsAction.OnResetFonts -> {
                    when (_localState.value.selectedFontType) {
                        FontType.APP -> fontPrefs.updateAppFont("Google Sans Flex")
                    }
                }
                is SettingsAction.OnMaterialThemeToggle -> themePrefs.updateMaterialTheme(action.pref)
                is SettingsAction.OnAppLockToggle -> privacyPrefs.updateAppLock(action.enabled)
                is SettingsAction.UpdateLockType -> privacyPrefs.updateLockType(action.type)
                is SettingsAction.UpdatePinHash -> privacyPrefs.updatePinHash(action.hash)
                is SettingsAction.OnScreenPrivacyToggle -> privacyPrefs.updateScreenPrivacy(action.enabled)
                is SettingsAction.OnAutoTimeToggle -> journalPrefs.setAutoTimeEnabled(action.enabled)
                is SettingsAction.OnStartOfWeekChange -> journalPrefs.setStartOfWeek(action.startOfWeek)
                is SettingsAction.OnTimeFormatChange -> journalPrefs.setTimeFormat(action.timeFormat)
                is SettingsAction.OnMapThemeChange -> journalPrefs.setMapTheme(action.theme)
                is SettingsAction.OnMarkdownToggle -> journalPrefs.setMarkdownEnabled(action.enabled)
            }
        }
    }
}