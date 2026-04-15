package com.denser.june.presentation.screens.settings.section

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState
import org.koin.compose.koinInject

@Composable
fun SyncBackupSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()

    SettingSection(title = "Sync & Backup") {
        SettingsItem(
            title = "Cloud Sync",
            subtitle = "Sync across devices via Cloud",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.cloud_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.SyncSettings) }
        )

        SettingsItem(
            title = "Local Backup",
            subtitle = "Restore or export data locally",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.drive_folder_upload_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.Backup) }
        )
    }
}
