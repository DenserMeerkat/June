package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.color.ColorTab
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.LocalSettingsTriggers
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SeedColorTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val triggers = LocalSettingsTriggers.current
    val enabled = !state.appTheme.materialTheme

    SettingsItem(
        title = stringResource(R.string.seed_color),
        subtitle = stringResource(R.string.seed_color_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.colors_24px),
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        },
        enabled = enabled,
        onClick = { triggers.onThemeSheetClick(ColorTab.COLOR) }
    )
}
