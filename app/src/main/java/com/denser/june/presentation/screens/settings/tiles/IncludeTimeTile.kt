package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneSwitchTile
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IncludeTimeTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    JuneSwitchTile(
        title = stringResource(R.string.include_time),
        iconRes = R.drawable.more_time_24px,
        checked = state.isAutoTimeEnabled,
        onCheckedChange = { viewModel.onAction(SettingsAction.OnAutoTimeToggle(it)) }
    )
}
