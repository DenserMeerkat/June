package com.denser.june

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.domain.model.AppTheme
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
    themePrefs: ThemePreferences,
    privacyPrefs: PrivacyPreferences,
    syncManager: SyncManager,
    syncPrefs: SyncPreferences
) : ViewModel() {

    private val themeFlow = combine(
        themePrefs.getSeedColorFlow(),
        themePrefs.getThemeMode(),
        themePrefs.getAmoledPrefFlow(),
        themePrefs.getPaletteStyle(),
        themePrefs.getMaterialYouFlow()
    ) { seed, themeMode, amoled, style, matYou ->
        AppTheme(
            seedColor = seed,
            themeMode = themeMode,
            withAmoled = amoled,
            style = style,
            materialTheme = matYou
        )
    }

    val state = combine(
        themeFlow,
        themePrefs.getFontFlow(),
        privacyPrefs.getAppLockFlow(),
        syncManager.status,
        syncPrefs.getSyncEnabled(),
        privacyPrefs.getIsInternetAllowedFlow()
    ) { args: Array<Any?> ->
        val baseTheme = args[0] as AppTheme
        val font = args[1] as com.denser.june.core.domain.model.enums.Fonts
        val appLock = args[2] as Boolean
        val syncStatus = args[3] as SyncStatus
        val syncEnabled = args[4] as Boolean
        val internetAllowed = args[5] as Boolean

        AppState(
            appTheme = baseTheme.copy(font = font),
            isAppLockEnabled = appLock,
            isLoading = false,
            syncStatus = syncStatus,
            isSyncEnabled = syncEnabled,
            isInternetAllowed = internetAllowed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState()
    )
}