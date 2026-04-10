package com.denser.june.presentation.screens.settings.section

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState
import com.denser.june.presentation.components.JuneConfirmationDialog
import org.koin.compose.koinInject

@Composable
fun GeneralSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()
    var showDeleteDialog by remember { mutableStateOf(false) }

    SettingSection(title = "General") {
        SettingsItem(
            title = "App Lock",
            subtitle = if (state.isAppLockEnabled) {
                if (state.lockType == LockType.PIN) "Custom PIN" else "Same as screen lock"
            } else {
                "No lock"
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.lock_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.LockMethod) }
        )

        SettingsItem(
            title = "Permissions",
            subtitle = "Manage app permissions",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.security_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.Permissions) }
        )

        SettingsItem(
            title = "Cloud Sync",
            subtitle = "Sync across devices via Cloud",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.cloud_sync_24px),
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
                    painter = painterResource(R.drawable.backup_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.Backup) }
        )

        SettingsItem(
            title = "Bin",
            subtitle = "View and restore deleted journals",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.Bin) }
        )

        SettingsItem(
            title = "Delete all journals",
            subtitle = "Move all entries to Bin",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.warning_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { showDeleteDialog = true }
        )
    }

    if (showDeleteDialog) {
        JuneConfirmationDialog(
            title = "Move all to Bin?",
            description = "This will move all your journal entries to the Bin. You can restore them within 30 days.",
            confirmText = "Delete",
            confirmButtonText = "Move All to Bin",
            onDismiss = { showDeleteDialog = false },
            onConfirm = { onAction(SettingsAction.OnDeleteJournals) }
        )
    }
}