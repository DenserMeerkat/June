package com.denser.june.presentation.screens.settings.components.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.AutoBackupInterval
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList
import com.denser.june.presentation.components.JuneCheckboxTile
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import kotlin.math.roundToInt

private val RETENTION_STOPS = listOf(1, 3, 5, 10, 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupOptionsBottomSheet(
    onDismiss: () -> Unit,
    folderName: String?,
    onSelectFolder: () -> Unit,
    onClearFolder: () -> Unit,
    interval: AutoBackupInterval,
    retentionCount: Int,
    requireCharging: Boolean,
    includeMedia: Boolean,
    includeAudios: Boolean,
    includeSongs: Boolean,
    includeSettings: Boolean,
    onTriggerBackupNow: () -> Unit,
    onSave: (
        interval: AutoBackupInterval,
        retentionCount: Int,
        requireCharging: Boolean,
        includeMedia: Boolean,
        includeAudios: Boolean,
        includeSongs: Boolean,
        includeSettings: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Buffered local states for smooth instantaneous toggles
    var localInterval by remember(interval) { mutableStateOf(interval) }
    var localRetentionCount by remember(retentionCount) { mutableIntStateOf(retentionCount) }
    var localRequireCharging by remember(requireCharging) { mutableStateOf(requireCharging) }
    var localIncludeMedia by remember(includeMedia) { mutableStateOf(includeMedia) }
    var localIncludeAudios by remember(includeAudios) { mutableStateOf(includeAudios) }
    var localIncludeSongs by remember(includeSongs) { mutableStateOf(includeSongs) }
    var localIncludeSettings by remember(includeSettings) { mutableStateOf(includeSettings) }

    val currentStopIndex = remember(localRetentionCount) {
        val idx = RETENTION_STOPS.indexOf(localRetentionCount)
        if (idx >= 0) idx.toFloat() else 1f
    }
    var sliderValue by remember(currentStopIndex) { mutableFloatStateOf(currentStopIndex) }

    val retentionLabel = remember(sliderValue) {
        val stop = RETENTION_STOPS[sliderValue.roundToInt().coerceIn(0, RETENTION_STOPS.lastIndex)]
        if (stop == 0) "All" else stop.toString()
    }

    val commitChanges = {
        onSave(
            localInterval,
            localRetentionCount,
            localRequireCharging,
            localIncludeMedia,
            localIncludeAudios,
            localIncludeSongs,
            localIncludeSettings
        )
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            commitChanges()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Fixed top header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.auto_backup),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Scrollable content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Folder Item (No section label, single row)
                    JuneConnectedList(
                        spacing = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        JuneConnectedItem(
                            title = folderName ?: stringResource(R.string.auto_backup_select_folder),
                            subtitle = null,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.folder_open_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                if (folderName != null) {
                                    IconButton(
                                        onClick = onClearFolder,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.close_24px),
                                            contentDescription = stringResource(R.string.action_clear),
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_forward_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            index = 0,
                            count = 1,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            onClick = onSelectFolder
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.schedule),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    JuneConnectedList(
                        spacing = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        JuneConnectedItem(
                            title = stringResource(R.string.auto_backup_frequency),
                            subtitle = null,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.manage_history),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            index = 0,
                            count = 3,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            JuneConnectedButtonGroup(
                                options = AutoBackupInterval.entries,
                                selected = { localInterval == it },
                                onSelect = { localInterval = it },
                                label = { interval ->
                                    val labelRes = when (interval) {
                                        AutoBackupInterval.DAILY -> R.string.interval_daily
                                        AutoBackupInterval.WEEKLY -> R.string.interval_weekly
                                        AutoBackupInterval.MONTHLY -> R.string.interval_monthly
                                    }
                                    stringResource(labelRes)
                                },
                                equalWeight = true,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Slider Item for Backups to Keep
                        JuneConnectedItem(
                            title = stringResource(R.string.auto_backup_retention),
                            subtitle = null,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rotate_right),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = retentionLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            index = 1,
                            count = 3,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = sliderValue,
                                onValueChange = { newVal ->
                                    sliderValue = newVal
                                    val stop = RETENTION_STOPS[newVal.roundToInt().coerceIn(0, RETENTION_STOPS.lastIndex)]
                                    localRetentionCount = stop
                                },
                                valueRange = 0f..4f,
                                steps = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        JuneConnectedItem(
                            title = stringResource(R.string.auto_backup_require_charging),
                            subtitle = null,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.battery_charging_50_2),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = localRequireCharging,
                                    onCheckedChange = { localRequireCharging = it }
                                )
                            },
                            index = 2,
                            count = 3,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { localRequireCharging = !localRequireCharging }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.include),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    JuneConnectedList(
                        spacing = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        JuneCheckboxTile(
                            checked = localIncludeMedia,
                            title = stringResource(R.string.media),
                            onCheckedChange = { localIncludeMedia = it },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.photo_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            index = 0,
                            count = 4
                        )

                        JuneCheckboxTile(
                            checked = localIncludeAudios,
                            title = stringResource(R.string.audios),
                            onCheckedChange = { localIncludeAudios = it },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.mic_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            index = 1,
                            count = 4
                        )

                        JuneCheckboxTile(
                            checked = localIncludeSongs,
                            title = stringResource(R.string.songs),
                            onCheckedChange = { localIncludeSongs = it },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.music_note_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            index = 2,
                            count = 4
                        )

                        JuneCheckboxTile(
                            checked = localIncludeSettings,
                            title = stringResource(R.string.include_settings),
                            onCheckedChange = { localIncludeSettings = it },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.rule_settings),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            index = 3,
                            count = 4
                        )
                    }
                }
            }

            // Fixed bottom Floating Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val hasFolder = !folderName.isNullOrBlank()
                JuneFloatingActionBar {
                    JuneFloatingAction(
                        onClick = {
                            commitChanges()
                            Toast.makeText(context, R.string.auto_backup_running, Toast.LENGTH_SHORT).show()
                            onTriggerBackupNow()
                        },
                        label = stringResource(R.string.auto_backup_now),
                        icon = { Icon(painterResource(R.drawable.drive_folder_upload_24px), contentDescription = null) },
                        enabled = hasFolder,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    JuneFloatingAction(
                        onClick = {
                            commitChanges()
                            onDismiss()
                        },
                        label = stringResource(R.string.action_done),
                        icon = { Icon(painterResource(R.drawable.check_24px), contentDescription = null) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
