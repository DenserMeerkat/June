package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ForceLtrAppLayoutTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.force_ltr_layout),
        subtitle = stringResource(R.string.force_ltr_layout_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.format_textdirection_l_to_r_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Switch(
                checked = state.isForceLtrUi,
                onCheckedChange = { viewModel.onAction(SettingsAction.OnForceLtrUiToggle(it)) }
            )
        },
        onClick = {
            viewModel.onAction(SettingsAction.OnForceLtrUiToggle(!state.isForceLtrUi))
        }
    )
}
