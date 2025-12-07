package com.example.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.june.core.data.repository.NoteRepository
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.ExportState
import com.example.june.core.domain.backup.RestoreRepo
import com.example.june.core.domain.backup.RestoreResult
import com.example.june.core.domain.backup.RestoreState
import com.example.june.core.domain.AppPreferences
import com.example.june.core.presentation.screens.settings.SettingsAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsVM(
    private val stateLayer: StateLayer,
    private val repo: NoteRepository,
    private val prefs: AppPreferences,
    private val exportRepo: ExportRepo,
    private val restoreRepo: RestoreRepo
) : ViewModel() {

    private var observeFlowsJob: Job? = null

    private val _state = stateLayer.settingsState
    val state = _state.asStateFlow()
        .onStart { observeJob() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _state.value
        )

    private var observeJob: Job? = null

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {

                SettingsAction.OnDeleteNotes -> repo.deleteAllNotes()

                SettingsAction.OnExportNotes -> {
                    _state.update { it.copy(exportState = ExportState.Exporting) }

                    val result = exportRepo.exportToJson()
                    _state.update {
                        it.copy(
                            exportState = if (result != null)
                                ExportState.ExportReady(result)
                            else ExportState.Error
                        )
                    }
                }

                is SettingsAction.OnRestoreNotes -> {
                    _state.update { it.copy(restoreState = RestoreState.Restoring) }

                    when (val res = restoreRepo.restoreNotes(action.path)) {
                        is RestoreResult.Failure -> {
                            _state.update {
                                it.copy(
                                    restoreState = RestoreState.Failure(res.exceptionType)
                                )
                            }
                        }
                        RestoreResult.Success -> {
                            _state.update {
                                it.copy(restoreState = RestoreState.Restored)
                            }
                        }
                    }
                }

                SettingsAction.ResetBackup -> {
                    _state.update {
                        it.copy(
                            restoreState = RestoreState.Idle,
                            exportState = ExportState.Exporting
                        )
                    }
                }

                is SettingsAction.OnUpdateOnboardingDone ->
                    prefs.updateOnboardingDone(action.done)

                is SettingsAction.OnSeedColorChange ->
                    prefs.updateSeedColor(action.color)

                is SettingsAction.OnAmoledSwitch ->
                    prefs.updateAmoledPref(action.amoled)

                is SettingsAction.OnThemeSwitch ->
                    prefs.updateAppThemePref(action.appTheme)

                is SettingsAction.OnPaletteChange ->
                    prefs.updatePaletteStyle(action.style)

                is SettingsAction.OnFontChange ->
                    prefs.updateFont(action.fonts)

                is SettingsAction.OnMaterialThemeToggle ->
                    prefs.updateMaterialTheme(action.pref)
            }
        }
    }

    private fun observeJob() {
        observeFlowsJob?.cancel()
        observeFlowsJob = viewModelScope.launch {
            observeTheme().launchIn(this)

            prefs.getOnboardingDoneFlow()
                .onEach { pref ->
                    _state.update {
                        it.copy(
                            onBoardingDone = pref
                        )
                    }
                }
                .launchIn(this)

            prefs.getFontFlow()
                .onEach { pref ->
                    _state.update {
                        it.copy(
                            theme = it.theme.copy(
                                font = pref
                            )
                        )
                    }
                }
                .launchIn(this)

            prefs.getMaterialYouFlow()
                .onEach { pref ->
                    _state.update {
                        it.copy(
                            theme = it.theme.copy(
                                materialTheme = pref
                            )
                        )
                    }
                }
                .launchIn(this)
        }
    }

    private fun observeTheme() : Flow<Unit> {
            return combine(
                prefs.getSeedColorFlow(),
                prefs.getAppThemePrefFlow(),
                prefs.getAmoledPrefFlow(),
                prefs.getPaletteStyle(),
                prefs.getFontFlow(),
            ) { seed, theme, amoled, style, font ->
                _state.update {
                    it.copy(
                        theme = it.theme.copy(
                            seedColor = seed,
                            appTheme = theme,
                            withAmoled = amoled,
                            style = style,
                            font = font
                        )
                    )
                }
            }
    }
}
