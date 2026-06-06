package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.koinInject

@Composable
fun MapSettingsTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = "Map settings",
        subtitle = "Select style and manage API keys",
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.map_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = { navigator.navigateTo(Route.MapSettings) }
    )
}
