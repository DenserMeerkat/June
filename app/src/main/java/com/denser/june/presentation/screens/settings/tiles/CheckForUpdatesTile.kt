package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.components.LocalSettingsTriggers
import com.denser.june.presentation.screens.settings.components.SettingsItem

@Composable
fun CheckForUpdatesTile() {
    val triggers = LocalSettingsTriggers.current
    SettingsItem(
        title = "Check for Updates",
        subtitle = "Check if a newer version is available",
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.sync_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = { triggers.onCheckForUpdatesClick() }
    )
}
