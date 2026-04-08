package com.denser.june

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.domain.model.AppTheme
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
    val isSyncEnabled: Boolean = false
)

class MainVM(
    themePrefs: ThemePreferences,
    privacyPrefs: PrivacyPreferences,
    private val syncManager: SyncManager,
    private val syncPrefs: com.denser.june.core.domain.preferences.SyncPreferences
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
        syncPrefs.getSyncEnabled()
    ) { baseTheme, font, appLock, syncStatus, syncEnabled ->
        AppState(
            appTheme = baseTheme.copy(font = font),
            isAppLockEnabled = appLock,
            isLoading = false,
            syncStatus = syncStatus,
            isSyncEnabled = syncEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState()
    )
}