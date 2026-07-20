package com.denser.june.presentation.screens.settings.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.backup.ExportState
import com.denser.june.core.domain.backup.RestoreState
import com.denser.june.core.domain.backup.RestoreFailedException
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.*
import com.denser.june.presentation.screens.settings.tiles.CloudSyncTile
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SyncBackupSettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state = settingsVM.state.collectAsStateWithLifecycle().value
    val onAction = settingsVM::onAction
    val context = LocalContext.current
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showExportDialog by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { targetUri ->
            if (state.exportState is ExportState.ExportReady) {
                try {
                    val tempFile = state.exportState.file
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                    Toast.makeText(context, "Backup saved successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                } finally {
                    onAction(SettingsAction.ResetBackup)
                }
            }
        }
    }

    val saveMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { targetUri ->
            if (state.exportMarkdownState is ExportState.ExportReady) {
                try {
                    val tempFile = state.exportMarkdownState.file
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                    Toast.makeText(context, "Markdown export saved successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                } finally {
                    onAction(SettingsAction.ResetBackup)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            showRestoreWarning = it.toString()
        }
    }

    LaunchedEffect(state.exportState) {
        if (state.exportState is ExportState.ExportReady) {
            val fileName = "June_Backup_${System.currentTimeMillis()}.zip"
            saveLauncher.launch(fileName)
        }
    }

    LaunchedEffect(state.exportMarkdownState) {
        if (state.exportMarkdownState is ExportState.ExportReady) {
            val fileName = "June_Markdown_Export_${System.currentTimeMillis()}.zip"
            saveMarkdownLauncher.launch(fileName)
        }
    }

    LaunchedEffect(state.restoreState) {
        when (state.restoreState) {
            is RestoreState.Restored -> {
                Toast.makeText(context, "Restore Complete!", Toast.LENGTH_SHORT).show()
                onAction(SettingsAction.ResetBackup)
            }

            is RestoreState.Failure -> {
                val errorMsg = when (state.restoreState.exception) {
                    RestoreFailedException.InvalidFile -> "Invalid or Corrupted Backup File"
                    RestoreFailedException.OldSchema -> "Backup format is too old"
                }
                Toast.makeText(context, "Restore Failed: $errorMsg", Toast.LENGTH_LONG).show()
                onAction(SettingsAction.ResetBackup)
            }

            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Sync & Backup") },
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        ) {
            item {
                SettingSection {
                    CloudSyncTile()
                }
            }
            item {
                SettingSection {
                    val isExporting = state.exportState is ExportState.Exporting || state.exportMarkdownState is ExportState.Exporting
                    SettingsItem(
                        title = "Export Data",
                        subtitle = if (isExporting) "Exporting..." else "Save your journals and media to a secure file.",
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.upload_24px),
                                null
                            )
                        },
                        trailingContent = if (isExporting) {
                            { CircularWavyProgressIndicator(modifier = Modifier.size(20.dp)) }
                        } else null,
                        enabled = !isExporting,
                        onClick = {
                            if (!isExporting) {
                                showExportDialog = true
                            }
                        }
                    )

                    val isRestoring = state.restoreState is RestoreState.Restoring
                    SettingsItem(
                        title = "Restore Data",
                        subtitle = if (isRestoring) "Restoring..." else "Import data from a previously saved backup file.",
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.download_24px),
                                null
                            )
                        },
                        trailingContent = if (isRestoring) {
                            { CircularWavyProgressIndicator(modifier = Modifier.size(20.dp)) }
                        } else null,
                        enabled = !isRestoring,
                        onClick = {
                            if (!isRestoring) {
                                restoreLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/json",
                                        "*/*"
                                    )
                                )
                            }
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showExportDialog) {
        ExportBottomSheet(
            onDismiss = { showExportDialog = false },
            onExport = { format, includeMedia ->
                if (format == ExportFormat.JSON) {
                    onAction(SettingsAction.OnExportJournals(includeMedia))
                } else {
                    onAction(SettingsAction.OnExportMarkdown(includeMedia))
                }
            }
        )
    }

    if (showRestoreWarning != null) {
        JuneDialog(
            onDismissRequest = { showRestoreWarning = null },
            title = "Restore Backup?",
            icon = R.drawable.cloud_sync_24px,
            confirmButton = {
                Button(
                    onClick = {
                        val uri = showRestoreWarning!!
                        showRestoreWarning = null
                        onAction(SettingsAction.OnRestoreJournals(uri))
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreWarning = null }) { Text("Cancel") }
            },
            text = {
                Text("This will merge the backup with your current data.\n\n• Entries with matching IDs will be OVERWRITTEN.\n• New entries will be ADDED.\n\nThis action cannot be undone.")
            }
        )
    }
}
