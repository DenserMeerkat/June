package com.denser.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.data.repository.JournalRepository
import com.denser.june.core.domain.AppPreferences
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.ExportState
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.domain.backup.RestoreResult
import com.denser.june.core.domain.backup.RestoreState
import com.denser.june.core.domain.enums.AppTheme
import com.denser.june.core.domain.enums.Fonts
import com.denser.june.core.domain.enums.LockType
import com.denser.june.core.domain.utils.FileUtils
import com.denser.june.core.presentation.screens.settings.SettingsAction
import com.denser.june.core.presentation.screens.settings.SettingsState
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
    private val prefs: AppPreferences,
    private val exportRepo: ExportRepo,
    private val restoreRepo: RestoreRepo
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsState())

    private val colorPrefsFlow = combine(
        prefs.getSeedColorFlow(),
        prefs.getAppThemePrefFlow(),
        prefs.getAmoledPrefFlow(),
        prefs.getPaletteStyle()
    ) { seed, appTheme, amoled, style ->
        ColorPrefs(seed, appTheme, amoled, style)
    }

    private val uiPrefsFlow = combine(
        prefs.getMaterialYouFlow(),
        prefs.getFontFlow(),
        prefs.getOnboardingDoneFlow()
    ) { matYou, font, onboarding ->
        UiPrefs(matYou, font, onboarding)
    }

    private val securityPrefsFlow = combine(
        prefs.getAppLockFlow(),
        prefs.getLockTypeFlow(),
        prefs.getPinHashFlow()
    ) { appLock, lockType, pinHash ->
        SecurityPrefs(appLock, lockType, pinHash)
    }

    val state = combine(
        _localState,
        colorPrefsFlow,
        uiPrefsFlow,
        securityPrefsFlow
    ) { local, colors, ui, security ->
        local.copy(
            onBoardingDone = ui.onboardingDone,
            isAppLockEnabled = security.appLock,
            lockType = security.lockType,
            pinHash = security.pinHash,

            theme = local.theme.copy(
                seedColor = colors.seed,
                appTheme = colors.appTheme,
                withAmoled = colors.amoled,
                style = colors.style,
                materialTheme = ui.matYou,
                font = ui.font,
            )
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsState()
    )

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                SettingsAction.OnDeleteJournals -> {
                    withContext(Dispatchers.IO) {
                        val allJournals = repo.getAllJournals()
                        allJournals.flatMap { it.images }.forEach { FileUtils.deleteMedia(it) }
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
                is SettingsAction.OnUpdateOnboardingDone -> prefs.updateOnboardingDone(action.done)
                is SettingsAction.OnSeedColorChange -> prefs.updateSeedColor(action.color)
                is SettingsAction.OnAmoledSwitch -> prefs.updateAmoledPref(action.amoled)
                is SettingsAction.OnThemeSwitch -> prefs.updateAppThemePref(action.appTheme)
                is SettingsAction.OnPaletteChange -> prefs.updatePaletteStyle(action.style)
                is SettingsAction.OnFontChange -> prefs.updateFont(action.fonts)
                is SettingsAction.OnMaterialThemeToggle -> prefs.updateMaterialTheme(action.pref)
                is SettingsAction.OnAppLockToggle -> prefs.updateAppLock(action.enabled)
                is SettingsAction.UpdateLockType -> prefs.updateLockType(action.type)
                is SettingsAction.UpdatePinHash -> prefs.updatePinHash(action.hash)
            }
        }
    }

    private data class ColorPrefs(
        val seed: Int,
        val appTheme: AppTheme,
        val amoled: Boolean,
        val style: PaletteStyle
    )

    private data class UiPrefs(
        val matYou: Boolean,
        val font: Fonts,
        val onboardingDone: Boolean
    )

    private data class SecurityPrefs(
        val appLock: Boolean,
        val lockType: LockType,
        val pinHash: String?
    )
}