package com.denser.june.presentation.screens.settings.section

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState
import com.denser.june.presentation.components.JuneConfirmationDialog
import com.denser.june.core.domain.model.enums.TimeFormat
import org.koin.compose.koinInject
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()
    var showDeleteDialog by remember { mutableStateOf(false) }

    SettingSection(title = "General") {
        SettingsItem(
            title = "Start of the week",
            subtitle = state.startOfWeek.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.event_note_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.End)
            ) {
                Spacer(Modifier.width(16.dp))
                val days = DayOfWeek.entries
                days.forEachIndexed { index, day ->
                    val isSelected = state.startOfWeek == day
                    val shape = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        days.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }

                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onAction(SettingsAction.OnStartOfWeekChange(day)) },
                        shapes = shape,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        )
                    ) {
                        Text(
                            text = day.getDisplayName(TextStyle.NARROW, LocalLocale.current.platformLocale),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        SettingsItem(
            title = "Time format",
            subtitle = if (state.timeFormat == TimeFormat.TWELVE_HOUR) "12-Hour" else "24-Hour",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.schedule_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                val options = TimeFormat.entries
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.End)
                ) {
                    options.forEachIndexed { index, format ->
                        val isSelected = state.timeFormat == format
                        val shape = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }

                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = { onAction(SettingsAction.OnTimeFormatChange(format)) },
                            shapes = shape,
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            )
                        ) {
                            Text(
                                text = if (format == TimeFormat.TWELVE_HOUR) "12 Hr" else "24 Hr",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        )

        SettingsItem(
            title = "Include time",
            subtitle = "Always enable time when creating journals",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.more_time_24px),
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
            title = "Reminders",
            subtitle = "Set journaling reminders",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.notifications_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.ReminderSettings) }
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

}
