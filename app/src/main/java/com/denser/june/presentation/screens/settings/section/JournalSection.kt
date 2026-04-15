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
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState
import com.denser.june.presentation.components.JuneConfirmationDialog
import org.koin.compose.koinInject

@Composable
fun JournalSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()
    var showDeleteDialog by remember { mutableStateOf(false) }

    SettingSection(title = "Journals") {
        SettingsItem(
            title = "Bin",
            subtitle = "Restore deleted journals",
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
