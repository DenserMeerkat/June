package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DefaultMapThemeTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.default_map_theme),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.routine_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    ) {
        JuneConnectedButtonGroup(
            options = MapTheme.entries,
            selected = { state.mapTheme == it },
            onSelect = { viewModel.onAction(SettingsAction.OnMapThemeChange(it)) },
            modifier = Modifier.fillMaxWidth()
        ) { mode, _ ->
            val iconRes = when (mode) {
                MapTheme.APP -> R.drawable.devices_24px
                MapTheme.LIGHT -> R.drawable.light_mode_24px
                MapTheme.DARK -> R.drawable.dark_mode_24px
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(mode.stringRes),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(mode.stringRes),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
