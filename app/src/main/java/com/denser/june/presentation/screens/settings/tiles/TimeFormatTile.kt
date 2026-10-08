package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TimeFormatTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.time_format),
        subtitle = if (state.timeFormat == TimeFormat.TWELVE_HOUR) stringResource(R.string.time_format_12h) else stringResource(R.string.time_format_24h),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.schedule_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            JuneConnectedButtonGroup(
                options = TimeFormat.entries,
                selected = { state.timeFormat == it },
                onSelect = { viewModel.onAction(SettingsAction.OnTimeFormatChange(it)) },
                label = { if (it == TimeFormat.TWELVE_HOUR) "12 Hr" else "24 Hr" }
            )
        }
    )
}
