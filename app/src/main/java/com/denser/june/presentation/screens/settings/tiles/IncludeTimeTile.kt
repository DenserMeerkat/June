package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IncludeTimeTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.include_time),
        subtitle = stringResource(R.string.include_time_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.more_time_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Switch(
                checked = state.isAutoTimeEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.OnAutoTimeToggle(it)) }
            )
        },
        onClick = { viewModel.onAction(SettingsAction.OnAutoTimeToggle(!state.isAutoTimeEnabled)) }
    )
}
