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

    val leadingIcon = when (state.mapTheme) {
        MapTheme.APP -> R.drawable.devices_24px
        MapTheme.LIGHT -> R.drawable.light_mode_24px
        MapTheme.DARK -> R.drawable.dark_mode_24px
    }

    SettingsItem(
        title = stringResource(R.string.default_map_theme),
        leadingContent = {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        JuneConnectedButtonGroup(
            options = MapTheme.entries,
            selected = { state.mapTheme == it },
            onSelect = { viewModel.onAction(SettingsAction.OnMapThemeChange(it)) },
            label = { stringResource(it.stringRes) },
            equalWeight = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
