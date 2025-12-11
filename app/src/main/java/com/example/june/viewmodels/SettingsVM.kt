package com.example.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.june.core.data.repository.JournalRepository
import com.example.june.core.domain.AppPreferences
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.ExportState
import com.example.june.core.domain.backup.RestoreRepo
import com.example.june.core.domain.backup.RestoreResult
import com.example.june.core.domain.backup.RestoreState
import com.example.june.core.domain.enums.AppTheme
import com.example.june.core.presentation.screens.settings.SettingsAction
import com.example.june.core.presentation.screens.settings.SettingsState
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsVM(
    private val repo: JournalRepository,
    private val prefs: AppPreferences,
    private val exportRepo: ExportRepo,
    private val restoreRepo: RestoreRepo
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsState())

    private val themeParamsFlow = combine(
        prefs.getSeedColorFlow(),
        prefs.getAppThemePrefFlow(),
        prefs.getAmoledPrefFlow(),
        prefs.getPaletteStyle(),
        prefs.getMaterialYouFlow(),
    ) { seed, appTheme, amoled, style, materialYou ->
        ThemeFlow(seed, appTheme, amoled, style, materialYou)
    }


    val state = combine(
        _localState,
        prefs.getOnboardingDoneFlow(),
        prefs.getMaterialYouFlow(),
        prefs.getFontFlow(),
        themeParamsFlow
    ) { local, onboarding, matYou, font, themeParams ->
        local.copy(
            onBoardingDone = onboarding,
            theme = local.theme.copy(
                seedColor = themeParams.seed,
                appTheme = themeParams.appTheme,
                withAmoled = themeParams.amoled,
                style = themeParams.style,
                materialTheme = matYou,
                font = font,
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
                SettingsAction.OnDeleteJournals -> repo.deleteAllJournals()

                SettingsAction.OnExportJournals -> {
                    _localState.update { it.copy(exportState = ExportState.Exporting) }
                    val result = exportRepo.exportToJson()
                    _localState.update {
                        it.copy(
                            exportState = if (result != null)
                                ExportState.ExportReady(result)
                            else ExportState.Error
                        )
                    }
                }

                is SettingsAction.OnRestoreJournals -> {
                    _localState.update { it.copy(restoreState = RestoreState.Restoring) }
                    when (val res = restoreRepo.restoreJournals(action.path)) {
                        is RestoreResult.Failure -> {
                            _localState.update {
                                it.copy(restoreState = RestoreState.Failure(res.exceptionType))
                            }
                        }

                        RestoreResult.Success -> {
                            _localState.update {
                                it.copy(restoreState = RestoreState.Restored)
                            }
                        }
                    }
                }

                SettingsAction.ResetBackup -> {
                    _localState.update {
                        it.copy(restoreState = RestoreState.Idle, exportState = ExportState.Exporting)
                    }
                }

                is SettingsAction.OnUpdateOnboardingDone -> prefs.updateOnboardingDone(action.done)
                is SettingsAction.OnSeedColorChange -> prefs.updateSeedColor(action.color)
                is SettingsAction.OnAmoledSwitch -> prefs.updateAmoledPref(action.amoled)
                is SettingsAction.OnThemeSwitch -> prefs.updateAppThemePref(action.appTheme)
                is SettingsAction.OnPaletteChange -> prefs.updatePaletteStyle(action.style)
                is SettingsAction.OnFontChange -> prefs.updateFont(action.fonts)
                is SettingsAction.OnMaterialThemeToggle -> prefs.updateMaterialTheme(action.pref)
            }
        }
    }

    private data class ThemeFlow(
        val seed: Int,
        val appTheme: AppTheme,
        val amoled: Boolean,
        val style: PaletteStyle,
        val materialYou: Boolean
    )
}