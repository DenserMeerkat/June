package com.example.june.core.presentation.screens.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.example.june.core.domain.backup.ExportState
import com.example.june.core.domain.backup.RestoreState
import com.example.june.core.domain.data_classes.Theme

@Stable
@Immutable
data class SettingsState(
    val theme: Theme = Theme(),
    val deleteButtonEnabled: Boolean = true,
    val exportState: ExportState = ExportState.Idle,
    val restoreState: RestoreState = RestoreState.Idle,
    val onBoardingDone: Boolean = true,
    val isAppLockEnabled: Boolean = false,
)