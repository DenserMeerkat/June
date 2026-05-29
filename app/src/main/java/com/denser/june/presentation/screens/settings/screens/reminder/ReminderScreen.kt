package com.denser.june.presentation.screens.settings.screens.reminder

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.JuneDateTimePicker
import com.denser.june.presentation.components.JuneDateTimePickerMode
import com.denser.june.core.utils.combineDateAndTime
import com.denser.june.core.utils.toLocalTime
import java.time.LocalDate
import com.denser.june.presentation.screens.settings.section.SettingSection
import com.denser.june.presentation.screens.settings.section.SettingsItem
import com.denser.june.core.utils.toFullTime
import com.denser.june.core.domain.model.enums.TimeFormat
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReminderScreen() {
    val viewModel: ReminderVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showTimePicker by remember { mutableStateOf(false) }
    val is24Hour = state.timeFormat == TimeFormat.TWENTY_FOUR_HOUR

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleReminder(true)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                title = { Text("Reminders") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Stay consistent with your journaling by setting up reminders",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))


            SettingSection{
                SettingsItem(
                    title = "Journaling Reminders",
                    leadingContent = {
                        Icon(
                            painter = painterResource(if(state.isEnabled) R.drawable.notifications_24px else R.drawable.notifications_off_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.toggleReminder(enabled)
                                }
                            }
                        )
                    },
                    onClick = {
                        val newEnabled = !state.isEnabled
                        if (newEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.toggleReminder(newEnabled)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            SettingSection(title = "Schedule") {
                SettingsItem(
                    title = "Reminder time",
                    subtitle = LocalTime.parse(state.time).toFullTime(is24Hour = is24Hour),
                    enabled = state.isEnabled,
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.schedule_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = { showTimePicker = true }
                )

                SettingsItem(
                    title = "Repeat",
                    subtitle = getRepeatSummary(state.days),
                    enabled = state.isEnabled,
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.event_repeat_24px),
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
                            val isSelected = state.days.contains(day)
                            val shape = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                days.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }

                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleDay(day) },
                                enabled = state.isEnabled,
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
                                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val initialDateTime = remember(state.time) {
            combineDateAndTime(LocalDate.now(), LocalTime.parse(state.time))
        }
        JuneDateTimePicker(
            initialDateTimeMillis = initialDateTime,
            mode = JuneDateTimePickerMode.TIME_ONLY,
            is24Hour = is24Hour,
            onDateTimeSelected = { millis ->
                viewModel.updateTime(millis.toLocalTime())
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

private fun getRepeatSummary(days: Set<DayOfWeek>): String {
    return when {
        days.size == 7 -> "Daily"
        days.isEmpty() -> "None"
        days.size == 5 && !days.contains(DayOfWeek.SATURDAY) && !days.contains(DayOfWeek.SUNDAY) -> "Weekdays"
        days.size == 2 && days.contains(DayOfWeek.SATURDAY) && days.contains(DayOfWeek.SUNDAY) -> "Weekends"
        else -> {
            days.sortedBy { it.value }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        }
    }
}
