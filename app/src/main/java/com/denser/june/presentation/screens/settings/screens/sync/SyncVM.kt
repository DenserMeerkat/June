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
    val isAutoSyncOn: Boolean = true,
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
    val showAdvancedOptions: Boolean = false
)

class SyncVM(
    private val syncPrefs: SyncPreferences,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(SyncSettingsState())
    
    private val _effect = MutableSharedFlow<SyncEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            val enabled = syncPrefs.getSyncEnabled().first()
            val auto = syncPrefs.isAutomaticSyncEnabled().first()
            val onlyWifi = syncPrefs.getSyncOnlyOnWifi().first()
            val url = syncPrefs.getWebDavUrl().first() ?: ""
            val user = syncPrefs.getWebDavUsername().first() ?: ""
            val pass = syncPrefs.getWebDavPassword().first() ?: ""
            val last = syncPrefs.getLastSyncTime().first()

            _state.update {
                it.copy(
                    isEnabled = enabled,
                    isAutoSyncOn = auto,
                    syncOnlyOnWifi = onlyWifi,
                    webDavUrl = url,
                    webDavUser = user,
                    webDavPass = pass,
                    lastSyncTime = last,
                    isConfigLocked = url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
                )
            }
        }
    }

    val state: StateFlow<SyncSettingsState> = combine(
        combine(
            syncPrefs.getSyncEnabled(),
            syncPrefs.isAutomaticSyncEnabled(),
            syncPrefs.getSyncOnlyOnWifi(),
            syncPrefs.getLastSyncTime()
        ) { enabled, auto, onlyWifi, last -> listOf(enabled, auto, onlyWifi, last) },
        combine(syncManager.status, _state) { syncStatus, internalState -> Pair(syncStatus, internalState) }
    ) { prefs, statusAndState ->
        val (syncStatus, internalState) = statusAndState
        @Suppress("UNCHECKED_CAST")
        internalState.copy(
            isEnabled = prefs[0] as Boolean,
            isAutoSyncOn = prefs[1] as Boolean,
            syncOnlyOnWifi = prefs[2] as Boolean,
            lastSyncTime = prefs[3] as Long,
            status = syncStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncSettingsState())

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
        // Only validate when trying to LOCK (going from unlocked to locked)
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
}
