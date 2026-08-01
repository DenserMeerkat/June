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

import com.denser.june.core.domain.preferences.JournalPreferences

data class AppState(
    val appTheme: AppTheme = AppTheme(),
    val isAppLockEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val isSyncEnabled: Boolean = false,
    val isInternetAllowed: Boolean = true,
    val isForceLtrUi: Boolean = false
)

class MainVM(
    initialAppTheme: AppTheme,
    themePrefs: ThemePreferences,
    privacyPrefs: PrivacyPreferences,
    fontPrefs: FontPreferences,
    syncManager: SyncManager,
    syncPrefs: SyncPreferences,
    journalPrefs: JournalPreferences
) : ViewModel() {

    val state = combine<Any?, AppState>(
        listOf(
            themePrefs.getAppThemeFlow(fontPrefs),
            privacyPrefs.getAppLockFlow(),
            syncManager.status,
            syncPrefs.getSyncEnabled(),
            privacyPrefs.getIsInternetAllowedFlow(),
            journalPrefs.isForceLtrUiEnabled()
        )
    ) { array ->
        AppState(
            appTheme = array[0] as AppTheme,
            isAppLockEnabled = array[1] as Boolean,
            isLoading = false,
            syncStatus = array[2] as SyncStatus,
            isSyncEnabled = array[3] as Boolean,
            isInternetAllowed = array[4] as Boolean,
            isForceLtrUi = array[5] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState(appTheme = initialAppTheme)
    )
}