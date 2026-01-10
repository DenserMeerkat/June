package com.denser.june.core.presentation.screens.settings.section

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.denser.june.R
import com.denser.june.core.navigation.AppNavigator
import com.denser.june.core.navigation.Route
import com.denser.june.core.presentation.screens.settings.SettingsAction
import com.denser.june.core.presentation.screens.settings.SettingsState
import com.denser.june.core.presentation.screens.settings.components.DeleteConfirmationDialog
import com.denser.june.core.presentation.screens.settings.components.PermissionsSheet
import org.koin.compose.koinInject

@Composable
fun GeneralSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }

    SettingSection(title = "General") {
        SettingsItem(
            title = "App Lock",
            subtitle = "Uses system biometric authentication",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.password_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.isAppLockEnabled,
                    onCheckedChange = { isChecked ->
                        onAction(SettingsAction.OnAppLockToggle(isChecked))
                    }
                )
            },
            onClick = {
                onAction(SettingsAction.OnAppLockToggle(!state.isAppLockEnabled))
            }
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
            onClick = { showPermissionsSheet = true }
        )

        SettingsItem(
            title = "Backup & Restore",
            subtitle = "Export or import your data",
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
            title = "Delete all journals",
            subtitle = "Permanently remove all entries",
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

    if (showPermissionsSheet) {
        PermissionsSheet(
            onDismiss = { showPermissionsSheet = false }
        )
    }
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { onAction(SettingsAction.OnDeleteJournals) }
        )
    }

}