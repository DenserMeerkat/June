package com.denser.june

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.domain.preferences.FontPreferences
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.core.domain.model.getAppThemeFlow
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.sync.SyncManager
import com.denser.june.core.domain.sync.SyncStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppState(
    val appTheme: AppTheme = AppTheme(),
    val isAppLockEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isSyncEnabled: Boolean = false,
    val isInternetAllowed: Boolean = true
)

class MainVM(
    initialAppTheme: AppTheme,
    themePrefs: ThemePreferences,
    privacyPrefs: PrivacyPreferences,
    fontPrefs: FontPreferences,
    syncManager: SyncManager,
    syncPrefs: SyncPreferences
) : ViewModel() {

    val state = combine(
        themePrefs.getAppThemeFlow(fontPrefs),
        privacyPrefs.getAppLockFlow(),
        syncManager.status,
        syncPrefs.getSyncEnabled(),
        privacyPrefs.getIsInternetAllowedFlow()
    ) { appTheme, isAppLockEnabled, syncStatus, isSyncEnabled, isInternetAllowed ->
        AppState(
            appTheme = appTheme,
            isAppLockEnabled = isAppLockEnabled,
            isLoading = false,
            syncStatus = syncStatus,
            isSyncEnabled = isSyncEnabled,
            isInternetAllowed = isInternetAllowed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState(appTheme = initialAppTheme)
    )
}