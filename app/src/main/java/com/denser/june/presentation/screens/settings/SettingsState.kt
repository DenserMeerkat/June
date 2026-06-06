package com.denser.june.presentation.screens.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.denser.june.core.domain.backup.ExportState
import com.denser.june.core.domain.backup.RestoreState
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.model.enums.FontType
import java.time.DayOfWeek

@Stable
@Immutable
data class SettingsState(
    val appTheme: AppTheme = AppTheme(),
    val deleteButtonEnabled: Boolean = true,
    val exportState: ExportState = ExportState.Idle,
    val restoreState: RestoreState = RestoreState.Idle,
    val isAppLockEnabled: Boolean = false,
    val lockType: LockType = LockType.BIOMETRIC,
    val pinHash: String? = null,
    val isScreenPrivacyEnabled: Boolean = false,
    val isAutoTimeEnabled: Boolean = false,
    val startOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    val timeFormat: TimeFormat = TimeFormat.TWELVE_HOUR,
    val selectedFontType: FontType = FontType.APP,
    val pendingFontName: String? = null,
    val isInternetAllowed: Boolean = true,
    val mapTheme: MapTheme = MapTheme.APP,
    val isMarkdownEnabled: Boolean = true
)