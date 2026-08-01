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
import androidx.compose.ui.res.stringResource
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

    val backupSavedMsg = stringResource(R.string.backup_saved_successfully)
    val markdownExportSavedMsg = stringResource(R.string.markdown_export_saved_successfully)
    val failedToSaveMsg = stringResource(R.string.failed_to_save_file)
    val restoreCompleteMsg = stringResource(R.string.restore_complete)
    val invalidBackupMsg = stringResource(R.string.invalid_or_corrupted_backup_file)
    val oldSchemaMsg = stringResource(R.string.backup_format_too_old)

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
                    Toast.makeText(context, backupSavedMsg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, failedToSaveMsg, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, markdownExportSavedMsg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, failedToSaveMsg, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, restoreCompleteMsg, Toast.LENGTH_SHORT).show()
                onAction(SettingsAction.ResetBackup)
            }

            is RestoreState.Failure -> {
                val errorMsg = when (state.restoreState.exception) {
                    RestoreFailedException.InvalidFile -> invalidBackupMsg
                    RestoreFailedException.OldSchema -> oldSchemaMsg
                }
                Toast.makeText(context, context.getString(R.string.restore_failed, errorMsg), Toast.LENGTH_LONG).show()
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
                title = { Text(text = stringResource(R.string.sync_and_backup)) },
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
                            contentDescription = stringResource(R.string.back),
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
                        title = stringResource(R.string.export_data),
                        subtitle = if (isExporting) stringResource(R.string.exporting) else stringResource(R.string.export_data_desc),
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
                        title = stringResource(R.string.restore_data),
                        subtitle = if (isRestoring) stringResource(R.string.restoring) else stringResource(R.string.restore_data_desc),
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
            title = stringResource(R.string.restore_backup_title),
            icon = R.drawable.cloud_sync_24px,
            confirmButton = {
                Button(
                    onClick = {
                        val uri = showRestoreWarning!!
                        showRestoreWarning = null
                        onAction(SettingsAction.OnRestoreJournals(uri))
                    }
                ) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreWarning = null }) { Text(stringResource(R.string.cancel)) }
            },
            text = {
                Text(stringResource(R.string.restore_warning_message))
            }
        )
    }
}
