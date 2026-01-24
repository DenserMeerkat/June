package com.denser.june.core.presentation.screens.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.denser.june.core.domain.backup.ExportState
import com.denser.june.core.domain.backup.RestoreState
import com.denser.june.core.domain.data_classes.Theme
import com.denser.june.core.domain.enums.LockType

@Stable
@Immutable
data class SettingsState(
    val theme: Theme = Theme(),
    val deleteButtonEnabled: Boolean = true,
    val exportState: ExportState = ExportState.Idle,
    val restoreState: RestoreState = RestoreState.Idle,
    val onBoardingDone: Boolean = true,
    val isAppLockEnabled: Boolean = false,
    val lockType: LockType = LockType.BIOMETRIC,
    val pinHash: String? = null
)