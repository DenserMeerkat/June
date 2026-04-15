package com.denser.june.presentation.screens.settings.section

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState
import com.denser.june.presentation.screens.settings.components.DayOfWeekPickerDialog
import com.denser.june.presentation.components.JuneConfirmationDialog
import org.koin.compose.koinInject
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun GeneralSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    SettingSection(title = "General") {
        SettingsItem(
            title = "Automatic time",
            subtitle = "Always capture current time when creating journals",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.schedule_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.isAutoTimeEnabled,
                    onCheckedChange = { onAction(SettingsAction.OnAutoTimeToggle(it)) }
                )
            },
            onClick = { onAction(SettingsAction.OnAutoTimeToggle(!state.isAutoTimeEnabled)) }
        )

        SettingsItem(
            title = "Start of the week",
            subtitle = state.startOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.event_note_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { showDayPicker = true }
        )
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

    if (showDayPicker) {
        DayOfWeekPickerDialog(
            currentDay = state.startOfWeek,
            onSelect = { onAction(SettingsAction.OnStartOfWeekChange(it)) },
            onDismiss = { showDayPicker = false }
        )
    }
}
