package com.denser.june.presentation.screens.settings.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsItem
import com.denser.june.presentation.screens.settings.components.ConsoleLogsBottomSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.denser.june.core.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperSettingsScreen() {
    val navigator = koinInject<AppNavigator>()
    val syncPrefs = koinInject<SyncPreferences>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val isSyncLoggingEnabled by syncPrefs.getSyncLoggingEnabled().collectAsState(initial = false)
    val isBackupLoggingEnabled by syncPrefs.getBackupLoggingEnabled().collectAsState(initial = false)
    val isDatabaseLoggingEnabled by syncPrefs.getDatabaseLoggingEnabled().collectAsState(initial = false)

    var showLogSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.developer_options)) },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = true,
                        onCheckedChange = { isEnabled ->
                            if (!isEnabled) {
                                scope.launch {
                                    syncPrefs.setDeveloperModeEnabled(false)
                                    syncPrefs.setSyncLoggingEnabled(false)
                                    syncPrefs.setBackupLoggingEnabled(false)
                                    syncPrefs.setDatabaseLoggingEnabled(false)
                                    AppLogger.clearBufferedLogs()
                                    navigator.navigateBack()
                                    Toast.makeText(context, "Developer options disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .scale(0.85f),
                        thumbContent = {
                            Icon(
                                painterResource(R.drawable.code_24px),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.console_logs)) },
                icon = { Icon(painterResource(R.drawable.code_24px), contentDescription = null) },
                onClick = {
                    showLogSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 80.dp + innerPadding.calculateBottomPadding())
        ) {
            item {
                Text(
                    text = "Configure developer logging options and view system console logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
            }

            item {
                SettingSection(stringResource(R.string.logging)) {
                    SettingsItem(
                        title = stringResource(R.string.sync_logging),
                        subtitle = stringResource(R.string.sync_logging_desc),
                        onClick = {
                            scope.launch { syncPrefs.setSyncLoggingEnabled(!isSyncLoggingEnabled) }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.sync_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isSyncLoggingEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { syncPrefs.setSyncLoggingEnabled(enabled) }
                                }
                            )
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.backup_logging),
                        subtitle = stringResource(R.string.backup_logging_desc),
                        onClick = {
                            scope.launch { syncPrefs.setBackupLoggingEnabled(!isBackupLoggingEnabled) }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.drive_folder_upload_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isBackupLoggingEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { syncPrefs.setBackupLoggingEnabled(enabled) }
                                }
                            )
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.database_logging),
                        subtitle = stringResource(R.string.database_logging_desc),
                        onClick = {
                            scope.launch { syncPrefs.setDatabaseLoggingEnabled(!isDatabaseLoggingEnabled) }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.database_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isDatabaseLoggingEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { syncPrefs.setDatabaseLoggingEnabled(enabled) }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showLogSheet) {
        ConsoleLogsBottomSheet(
            onDismissRequest = { showLogSheet = false }
        )
    }
}
