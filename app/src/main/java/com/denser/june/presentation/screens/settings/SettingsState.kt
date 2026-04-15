package com.denser.june.presentation.screens.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.denser.june.core.domain.backup.ExportState
import com.denser.june.core.domain.backup.RestoreState
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.core.domain.model.enums.LockType

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
    val isScreenPrivacyEnabled: Boolean = false
)