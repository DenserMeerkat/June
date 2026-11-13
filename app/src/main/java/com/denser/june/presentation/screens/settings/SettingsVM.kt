package com.denser.june.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.domain.preferences.FontPreferences
import com.denser.june.core.domain.backup.BackupCoordinator
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.MarkdownImportRepo
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.domain.backup.SettingsBackupRepo
import com.denser.june.core.domain.importers.AppImportRepo
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.presentation.utils.AsyncOp
import com.denser.june.presentation.utils.toAsyncOp
import com.denser.june.core.domain.model.enums.EditorLayoutDirection
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
import com.denser.june.core.domain.billing.BillingHandler

class SettingsVM(
    private val repo: JournalRepository,
    private val themePrefs: ThemePreferences,
    private val privacyPrefs: PrivacyPreferences,
    private val journalPrefs: JournalPreferences,
    private val fontPrefs: FontPreferences,
    private val exportRepo: ExportRepo,
    private val restoreRepo: RestoreRepo,
    private val markdownImportRepo: MarkdownImportRepo,
    private val appImportRepo: AppImportRepo,
    private val billingHandler: BillingHandler,
    private val backupCoordinator: BackupCoordinator,
    private val settingsBackupRepo: SettingsBackupRepo
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsState())

    init {
        viewModelScope.launch {
            backupCoordinator.backupStatus.collect { status ->
                _localState.update { it.copy(backupOperationStatus = status) }
            }
        }
        viewModelScope.launch {
            backupCoordinator.restoreStatus.collect { status ->
                _localState.update { it.copy(restoreOperationStatus = status) }
            }
        }
        viewModelScope.launch {
            settingsBackupRepo.hasExportableMapKeys().collect { hasKeys ->
                _localState.update { it.copy(hasMapKeys = hasKeys) }
            }
        }
        viewModelScope.launch {
            loadPreBackupCounts()
        }
    }

    private suspend fun loadPreBackupCounts() {
        val counts = runCatching { backupCoordinator.getPreBackupCounts() }.getOrNull()
        if (counts != null) {
            _localState.update { it.copy(preBackupCounts = counts) }
        }
    }

    suspend fun exportSettingsJson(includeMapKeys: Boolean): Result<String> {
        return settingsBackupRepo.exportSettingsJson(includeMapKeys)
    }

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
            fontPrefs.getAppFontVariation(),
            privacyPrefs.getIsInternetAllowedFlow(),
            journalPrefs.mapTheme(),
            journalPrefs.isMarkdownEnabled(),
            privacyPrefs.getSecurityQuestionFlow(),
            privacyPrefs.getSecurityAnswerHashFlow(),
            journalPrefs.alwaysOpenNewNote(),
            journalPrefs.isKeyboardAutocorrectEnabled(),
            journalPrefs.isKeyboardCapitalizationEnabled(),
            journalPrefs.editorLayoutDirection(),
            journalPrefs.isForceLtrUiEnabled(),
            journalPrefs.isCompressPhotosEnabled(),
            journalPrefs.getPhotoCompressionQuality(),
            journalPrefs.isCompressVideosEnabled(),
            journalPrefs.getVideoCompressionQuality(),
            journalPrefs.isCompressAudioEnabled(),
            journalPrefs.getAudioCompressionQuality(),
            privacyPrefs.getPasswordHashFlow(),
            privacyPrefs.getPatternHashFlow(),
            billingHandler.isPlus
        )
    ) { array ->
        val local = array[0] as SettingsState

        local.copy(
            isAppLockEnabled = array[6] as Boolean,
            lockType = array[7] as LockType,
            pinHash = array[8] as String?,
            passwordHash = array[31] as String?,
            patternHash = array[32] as String?,
            isPlus = array[33] as Boolean,
            isScreenPrivacyEnabled = array[9] as Boolean,
            isAutoTimeEnabled = array[10] as Boolean,
            startOfWeek = array[11] as DayOfWeek,
            timeFormat = array[12] as TimeFormat,
            isInternetAllowed = array[15] as Boolean,
            mapTheme = array[16] as MapTheme,
            isMarkdownEnabled = array[17] as Boolean,
            securityQuestion = array[18] as String?,
            securityAnswerHash = array[19] as String?,
            alwaysOpenNewNote = array[20] as Boolean,
            isKeyboardAutocorrectEnabled = array[21] as Boolean,
            isKeyboardCapitalizationEnabled = array[22] as Boolean,
            editorLayoutDirection = array[23] as EditorLayoutDirection,
            isForceLtrUi = array[24] as Boolean,
            isCompressPhotosEnabled = array[25] as Boolean,
            photoCompressionQuality = array[26] as Int,
            isCompressVideosEnabled = array[27] as Boolean,
            videoCompressionQuality = array[28] as Int,
            isCompressAudioEnabled = array[29] as Boolean,
            audioCompressionQuality = array[30] as Int,

            appTheme = local.appTheme.copy(
                seedColor = array[1] as Int,
                themeMode = array[2] as ThemeMode,
                withAmoled = array[3] as Boolean,
                style = array[4] as PaletteStyle,
                materialTheme = array[5] as Boolean,
                appFont = array[13] as String,
                appFontVariation = array[14] as? FontVariationConfig,
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

                SettingsAction.LoadPreBackupCounts -> {
                    loadPreBackupCounts()
                }

                is SettingsAction.OnExportJournals -> {
                    _localState.update { it.copy(exportState = AsyncOp.Loading) }
                    backupCoordinator.startBackup(
                        includeMedia = action.includeMedia,
                        includeAudios = action.includeAudios,
                        includeSongFiles = action.includeSongFiles,
                        includeSettings = action.includeSettings,
                        includeMapKeys = action.includeMapKeys
                    ) { result ->
                        _localState.update { it.copy(exportState = result.toAsyncOp()) }
                    }
                }

                is SettingsAction.OnExportMarkdown -> {
                    _localState.update { it.copy(exportMarkdownState = AsyncOp.Loading) }
                    val result = exportRepo.exportAsMarkdown(
                        includeMedia = action.includeMedia,
                        includeAudios = action.includeAudios,
                        includeSongFiles = action.includeSongs
                    )
                    _localState.update { it.copy(exportMarkdownState = result.toAsyncOp()) }
                }

                is SettingsAction.OnRestoreJournals -> {
                    _localState.update { it.copy(restoreState = AsyncOp.Loading) }
                    backupCoordinator.startRestore(action.path) { result ->
                        _localState.update { it.copy(restoreState = result.map {}.toAsyncOp()) }
                    }
                }

                is SettingsAction.OnRestoreSettings -> {
                    _localState.update { it.copy(settingsRestoreState = AsyncOp.Loading) }
                    val result = settingsBackupRepo.restoreSettingsJson(action.json)
                    _localState.update { it.copy(settingsRestoreState = result.toAsyncOp()) }
                }

                SettingsAction.ResetSettingsRestore -> _localState.update {
                    it.copy(settingsRestoreState = AsyncOp.Idle)
                }

                is SettingsAction.OnImportMarkdownFiles -> {
                    _localState.update { it.copy(importMarkdownState = AsyncOp.Loading) }
                    val result = markdownImportRepo.importMarkdownFiles(action.uris)
                    _localState.update { it.copy(importMarkdownState = result.toAsyncOp()) }
                }
                is SettingsAction.OnImportMarkdownZip -> {
                    _localState.update { it.copy(importMarkdownState = AsyncOp.Loading) }
                    val result = markdownImportRepo.importMarkdownZip(action.uri)
                    _localState.update { it.copy(importMarkdownState = result.toAsyncOp()) }
                }
                SettingsAction.ResetMarkdownImport -> _localState.update {
                    it.copy(importMarkdownState = AsyncOp.Idle)
                }
                is SettingsAction.OnAppImport -> {
                    _localState.update { it.copy(appImportState = AsyncOp.Loading) }
                    val result = appImportRepo.importFromUri(action.uri)
                    _localState.update { it.copy(appImportState = result.toAsyncOp()) }
                }
                SettingsAction.ResetAppImport -> _localState.update {
                    it.copy(appImportState = AsyncOp.Idle)
                }
                SettingsAction.ResetBackup -> {
                    backupCoordinator.resetBackupStatus()
                    backupCoordinator.resetRestoreStatus()
                    _localState.update {
                        it.copy(
                            restoreState = AsyncOp.Idle,
                            exportState = AsyncOp.Idle,
                            exportMarkdownState = AsyncOp.Idle,
                            importMarkdownState = AsyncOp.Idle,
                            appImportState = AsyncOp.Idle,
                            settingsRestoreState = AsyncOp.Idle
                        )
                    }
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
                is SettingsAction.OnAppFontVariationChange -> {
                    fontPrefs.updateAppFontVariation(action.variation)
                }
                SettingsAction.OnResetAppFontVariation -> {
                    fontPrefs.updateAppFontVariation(FontVariationConfig.DEFAULT)
                }
                SettingsAction.OnResetFonts -> {
                    when (_localState.value.selectedFontType) {
                        FontType.APP -> {
                            fontPrefs.updateAppFont("Google Sans Flex")
                            fontPrefs.updateAppFontVariation(FontVariationConfig.DEFAULT)
                        }
                    }
                }
                is SettingsAction.OnMaterialThemeToggle -> themePrefs.updateMaterialTheme(action.pref)
                is SettingsAction.OnAppLockToggle -> privacyPrefs.updateAppLock(action.enabled)
                is SettingsAction.UpdateLockType -> privacyPrefs.updateLockType(action.type)
                is SettingsAction.UpdatePinHash -> privacyPrefs.updatePinHash(action.hash)
                is SettingsAction.UpdatePasswordHash -> privacyPrefs.updatePasswordHash(action.hash)
                is SettingsAction.UpdatePatternHash -> privacyPrefs.updatePatternHash(action.hash)
                is SettingsAction.UpdateSecurityQuestionAndAnswer -> privacyPrefs.updateSecurityQuestionAndAnswer(action.question, action.answerHash)
                is SettingsAction.OnScreenPrivacyToggle -> privacyPrefs.updateScreenPrivacy(action.enabled)
                is SettingsAction.OnAutoTimeToggle -> journalPrefs.setAutoTimeEnabled(action.enabled)
                is SettingsAction.OnStartOfWeekChange -> journalPrefs.setStartOfWeek(action.startOfWeek)
                is SettingsAction.OnTimeFormatChange -> journalPrefs.setTimeFormat(action.timeFormat)
                is SettingsAction.OnMapThemeChange -> journalPrefs.setMapTheme(action.theme)
                is SettingsAction.OnMarkdownToggle -> journalPrefs.setMarkdownEnabled(action.enabled)
                is SettingsAction.OnAlwaysOpenNewNoteToggle -> journalPrefs.setAlwaysOpenNewNote(action.enabled)
                is SettingsAction.OnKeyboardAutocorrectToggle -> journalPrefs.setKeyboardAutocorrectEnabled(action.enabled)
                is SettingsAction.OnKeyboardCapitalizationToggle -> journalPrefs.setKeyboardCapitalizationEnabled(action.enabled)
                is SettingsAction.OnEditorLayoutDirectionChange -> journalPrefs.setEditorLayoutDirection(action.direction)
                is SettingsAction.OnForceLtrUiToggle -> journalPrefs.setForceLtrUiEnabled(action.enabled)
                is SettingsAction.OnCompressPhotosToggle -> journalPrefs.setCompressPhotosEnabled(action.enabled)
                is SettingsAction.OnPhotoQualityChange -> journalPrefs.setPhotoCompressionQuality(action.quality)
                is SettingsAction.OnCompressVideosToggle -> journalPrefs.setCompressVideosEnabled(action.enabled)
                is SettingsAction.OnVideoQualityChange -> journalPrefs.setVideoCompressionQuality(action.quality)
                is SettingsAction.OnCompressAudioToggle -> journalPrefs.setCompressAudioEnabled(action.enabled)
                is SettingsAction.OnAudioQualityChange -> journalPrefs.setAudioCompressionQuality(action.quality)
            }
        }
    }
}