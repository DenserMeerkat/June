package com.denser.june.presentation.screens.settings.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.sync.SyncManager
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.core.domain.sync.SyncAnalysis
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SyncEffect {
    data class ShowToast(val message: String) : SyncEffect()
}

data class SyncSettingsState(
    val isEnabled: Boolean = false,
    val isAutoSyncOn: Boolean = false,
    val syncOnlyOnWifi: Boolean = true,
    val webDavUrl: String = "",
    val webDavUser: String = "",
    val webDavPass: String = "",
    val lastSyncTime: Long = 0,
    val status: SyncStatus = SyncStatus.Idle,
    val isTestingConnection: Boolean = false,
    val isConfigLocked: Boolean = false,
    val webDavUrlError: String? = null,
    val webDavUserError: String? = null,
    val webDavPassError: String? = null,
    val analysis: SyncAnalysis? = null,
    val isAnalyzing: Boolean = false,
    val showAdvancedOptions: Boolean = false,
    val showAnalysisDetails: Boolean = false
)

class SyncVM(
    private val syncPrefs: SyncPreferences,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(SyncSettingsState())
    
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SyncEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            var isInitialLoad = true
            combine(
                syncPrefs.getSyncEnabled(),
                syncPrefs.isAutomaticSyncEnabled(),
                syncPrefs.getSyncOnlyOnWifi(),
                syncPrefs.getLastSyncTime(),
                syncPrefs.getWebDavUrl(),
                syncPrefs.getWebDavUsername(),
                syncPrefs.getWebDavPassword()
            ) { args: Array<Any?> -> args }
                .collect { args ->
                    val url = args[4] as String? ?: ""
                    val user = args[5] as String? ?: ""
                    val pass = args[6] as String? ?: ""
                    
                    _state.update {
                        it.copy(
                            isEnabled = args[0] as Boolean,
                            isAutoSyncOn = args[1] as Boolean,
                            syncOnlyOnWifi = args[2] as Boolean,
                            lastSyncTime = args[3] as Long,
                            webDavUrl = url,
                            webDavUser = user,
                            webDavPass = pass,
                            webDavUrlError = null,
                            webDavUserError = null,
                            webDavPassError = null,
                            isConfigLocked = if (isInitialLoad) {
                                url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
                            } else {
                                it.isConfigLocked
                            }
                        )
                    }
                    isInitialLoad = false
                }
        }

        viewModelScope.launch {
            syncManager.status.collect { status ->
                _state.update { it.copy(status = status) }
                when (status) {
                    is SyncStatus.Success -> {
                        analyzeSync()
                    }
                    is SyncStatus.Preparing, is SyncStatus.Syncing -> {
                        _state.update { it.copy(analysis = null) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun toggleSync(enabled: Boolean) {
        viewModelScope.launch { syncPrefs.setSyncEnabled(enabled) }
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch { syncPrefs.setAutomaticSyncEnabled(enabled) }
    }

    fun toggleSyncOnlyOnWifi(onlyWifi: Boolean) {
        viewModelScope.launch { syncPrefs.setSyncOnlyOnWifi(onlyWifi) }
    }

    fun updateUrl(url: String) {
        _state.update { it.copy(webDavUrl = url, webDavUrlError = null) }
        viewModelScope.launch { syncPrefs.setWebDavUrl(url) }
    }

    fun updateUser(user: String) {
        _state.update { it.copy(webDavUser = user, webDavUserError = null) }
        viewModelScope.launch { syncPrefs.setWebDavUsername(user) }
    }

    fun updatePass(pass: String) {
        _state.update { it.copy(webDavPass = pass, webDavPassError = null) }
        viewModelScope.launch { syncPrefs.setWebDavPassword(pass) }
    }

    fun manualSync() {
        if (!validateInputs()) return
        syncManager.launchSync()
    }

    fun revalidate() {
        if (!validateInputs()) return
        viewModelScope.launch {
            syncManager.sync(isFullRevalidation = true)
            analyzeSync()
        }
    }

    fun analyzeSync() {
        if (!validateInputs()) return
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, analysis = null) }
            val result = syncManager.performAnalysis()
            _state.update { it.copy(isAnalyzing = false) }
            
            result.onSuccess { analysis ->
                _state.update { it.copy(analysis = analysis) }
            }.onFailure {
                _effect.emit(SyncEffect.ShowToast("Analysis failed: ${it.message}"))
            }
        }
    }

    fun testConnection() {
        if (!validateInputs()) return

        viewModelScope.launch {
            try {
                _state.update { it.copy(isTestingConnection = true) }
                val result = syncManager.testProviderConnection("WebDAV")
                _state.update { it.copy(isTestingConnection = false) }

                if (result.isSuccess) {
                    _effect.emit(SyncEffect.ShowToast("Connection successful!"))
                } else {
                    _effect.emit(SyncEffect.ShowToast("Connection failed: ${result.exceptionOrNull()?.message}"))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isTestingConnection = false) }
                _effect.emit(SyncEffect.ShowToast("Unexpected error: ${e.message}"))
            }
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _state.value
        var isValid = true

        val urlError = if (currentState.webDavUrl.isBlank()) "Server URL cannot be empty" else null
        val userError = if (currentState.webDavUser.isBlank()) "Username cannot be empty" else null
        val passError = if (currentState.webDavPass.isBlank()) "Password cannot be empty" else null

        if (urlError != null || userError != null || passError != null) {
            isValid = false
            _state.update {
                it.copy(
                    webDavUrlError = urlError,
                    webDavUserError = userError,
                    webDavPassError = passError
                )
            }
        }

        return isValid
    }
    
    fun resetStatus() {
        syncManager.resetStatus()
    }

    fun toggleConfigLock() {
        val currentState = _state.value
        if (!currentState.isConfigLocked) {
            if (!validateInputs()) {
                viewModelScope.launch {
                    _effect.emit(SyncEffect.ShowToast("Please fill all WebDAV details before locking"))
                }
                return
            }
        }
        _state.update { it.copy(isConfigLocked = !it.isConfigLocked) }
    }

    fun toggleAdvancedOptions() {
        _state.update { it.copy(showAdvancedOptions = !it.showAdvancedOptions) }
    }

    fun setShowAnalysisDetails(show: Boolean) {
        _state.update { it.copy(showAnalysisDetails = show) }
    }
}
