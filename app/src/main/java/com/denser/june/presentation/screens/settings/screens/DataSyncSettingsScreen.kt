package com.denser.june.presentation.screens.settings.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.backup.BackupOperationStatus
import com.denser.june.core.domain.backup.RestoreException
import com.denser.june.presentation.components.BackupRestoreIndicator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneBackButton
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.*
import com.denser.june.presentation.screens.settings.components.sheets.*
import com.denser.june.presentation.screens.settings.tiles.CloudSyncTile
import com.denser.june.presentation.utils.AsyncOp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DataSyncSettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state = settingsVM.state.collectAsStateWithLifecycle().value
    val onAction = settingsVM::onAction
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showCreateBackupSheet by remember { mutableStateOf(false) }
    var showAutoBackupOptionsSheet by remember { mutableStateOf(false) }
    var showSettingsBackupSheet by remember { mutableStateOf(false) }
    var showExportMarkdownSheet by remember { mutableStateOf(false) }
    var showAppImportSheet by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf<String?>(null) }
    var pendingSettingsJsonToRestore by remember { mutableStateOf<String?>(null) }
    var pendingSettingsJsonToSave by remember { mutableStateOf<String?>(null) }

    val autoBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            onAction(SettingsAction.OnAutoBackupFolderSelected(it))
            onAction(SettingsAction.OnAutoBackupToggle(true))
        }
    }

    val backupSavedMsg = stringResource(R.string.backup_saved_successfully)
    val markdownExportSavedMsg = stringResource(R.string.markdown_export_saved_successfully)
    val settingsSavedMsg = stringResource(R.string.settings_saved_successfully)
    val settingsRestoredMsg = stringResource(R.string.settings_restored_successfully)
    val failedToSaveMsg = stringResource(R.string.failed_to_save_file)
    val restoreCompleteMsg = stringResource(R.string.restore_complete)
    val invalidBackupMsg = stringResource(R.string.invalid_or_corrupted_backup_file)
    val oldSchemaMsg = stringResource(R.string.backup_format_too_old)

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { targetUri ->
            if (state.exportState is AsyncOp.Success) {
                try {
                    val tempFile = state.exportState.data
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                    Toast.makeText(context, backupSavedMsg, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(context, failedToSaveMsg, Toast.LENGTH_SHORT).show()
                } finally {
                    onAction(SettingsAction.ResetBackup)
                }
            }
        }
    }

    val saveSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { targetUri ->
            pendingSettingsJsonToSave?.let { jsonString ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        output.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, settingsSavedMsg, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(context, failedToSaveMsg, Toast.LENGTH_SHORT).show()
                } finally {
                    pendingSettingsJsonToSave = null
                }
            }
        }
    }

    val saveMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { targetUri ->
            if (state.exportMarkdownState is AsyncOp.Success) {
                try {
                    val tempFile = state.exportMarkdownState.data
                    context.contentResolver.openOutputStream(targetUri)?.use { output ->
                        tempFile.inputStream().use { input -> input.copyTo(output) }
                    }
                    Toast.makeText(context, markdownExportSavedMsg, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
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

    val restoreSettingsFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { targetUri ->
            try {
                val jsonString = context.contentResolver.openInputStream(targetUri)?.use { input ->
                    input.bufferedReader().use { it.readText() }
                }
                if (!jsonString.isNullOrBlank()) {
                    pendingSettingsJsonToRestore = jsonString
                }
            } catch (_: Exception) {
                Toast.makeText(context, invalidBackupMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1 && (uris.first().toString().endsWith(".zip", ignoreCase = true) || context.contentResolver.getType(uris.first()) == "application/zip")) {
                onAction(SettingsAction.OnImportMarkdownZip(uris.first()))
            } else {
                onAction(SettingsAction.OnImportMarkdownFiles(uris))
            }
        }
    }

    val appImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            onAction(SettingsAction.OnAppImport(it))
        }
    }

    LaunchedEffect(state.exportState) {
        when (state.exportState) {
            is AsyncOp.Success -> {
                val fileName = "June_Backup_${System.currentTimeMillis()}.zip"
                saveLauncher.launch(fileName)
            }
            is AsyncOp.Error -> {
                Toast.makeText(context, failedToSaveMsg, Toast.LENGTH_LONG).show()
                onAction(SettingsAction.ResetBackup)
            }
            else -> Unit
        }
    }

    LaunchedEffect(state.exportMarkdownState) {
        when (state.exportMarkdownState) {
            is AsyncOp.Success -> {
                val fileName = "June_Markdown_Export_${System.currentTimeMillis()}.zip"
                saveMarkdownLauncher.launch(fileName)
            }
            is AsyncOp.Error -> {
                Toast.makeText(context, failedToSaveMsg, Toast.LENGTH_LONG).show()
                onAction(SettingsAction.ResetBackup)
            }
            else -> Unit
        }
    }

    LaunchedEffect(state.importMarkdownState) {
        when (val importState = state.importMarkdownState) {
            is AsyncOp.Success -> {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.markdown_imported_successfully, importState.data),
                    Toast.LENGTH_SHORT
                ).show()
                onAction(SettingsAction.ResetMarkdownImport)
            }

            is AsyncOp.Error -> {
                val err = importState.message ?: invalidBackupMsg
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.markdown_import_failed, err),
                    Toast.LENGTH_LONG
                ).show()
                onAction(SettingsAction.ResetMarkdownImport)
            }

            else -> Unit
        }
    }

    LaunchedEffect(state.restoreState) {
        when (val restoreState = state.restoreState) {
            is AsyncOp.Success -> {
                Toast.makeText(context, restoreCompleteMsg, Toast.LENGTH_SHORT).show()
                onAction(SettingsAction.ResetBackup)
            }

            is AsyncOp.Error -> {
                val errorMsg = when (restoreState.cause) {
                    RestoreException.InvalidFile -> invalidBackupMsg
                    RestoreException.OldSchema -> oldSchemaMsg
                    else -> restoreState.message ?: invalidBackupMsg
                }
                Toast.makeText(context, context.resources.getString(R.string.restore_failed, errorMsg), Toast.LENGTH_LONG).show()
                onAction(SettingsAction.ResetBackup)
            }

            else -> Unit
        }
    }

    LaunchedEffect(state.settingsRestoreState) {
        when (val settingsState = state.settingsRestoreState) {
            is AsyncOp.Success -> {
                Toast.makeText(context, settingsRestoredMsg, Toast.LENGTH_SHORT).show()
                onAction(SettingsAction.ResetSettingsRestore)
            }
            is AsyncOp.Error -> {
                val err = settingsState.message ?: invalidBackupMsg
                Toast.makeText(context, "Failed to restore settings: $err", Toast.LENGTH_LONG).show()
                onAction(SettingsAction.ResetSettingsRestore)
            }
            else -> Unit
        }
    }

    LaunchedEffect(state.appImportState) {
        when (val importState = state.appImportState) {
            is AsyncOp.Success -> {
                val summary = importState.data
                val countMsg = context.resources.getString(
                    R.string.app_import_summary_success,
                    summary.importedJournals,
                    summary.updatedJournals,
                    summary.mediaExtracted,
                    summary.audiosExtracted
                )
                val finalMsg = if (summary.versionWarning != null) {
                    "${summary.versionWarning}\n$countMsg"
                } else {
                    countMsg
                }
                Toast.makeText(context, finalMsg, Toast.LENGTH_LONG).show()
                onAction(SettingsAction.ResetAppImport)
            }

            is AsyncOp.Error -> {
                val err = importState.message ?: invalidBackupMsg
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.app_import_failed, err),
                    Toast.LENGTH_LONG
                ).show()
                onAction(SettingsAction.ResetAppImport)
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
                title = { Text(text = stringResource(R.string.data_and_sync)) },
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                },
                actions = {
                    BackupRestoreIndicator(
                        backupStatus = state.backupOperationStatus,
                        autoBackupStatus = state.autoBackupOperationStatus,
                        restoreStatus = state.restoreOperationStatus,
                        isExporting = state.exportState.isLoading,
                        isRestoring = state.restoreState.isLoading,
                        isExportingMarkdown = state.exportMarkdownState.isLoading,
                        isImportingMarkdown = state.importMarkdownState.isLoading,
                        isAppImporting = state.appImportState.isLoading
                    )
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
                SettingSection(
                    title = stringResource(R.string.backup_and_restore)
                ) {
                    val isBackingUp = state.exportState.isLoading || state.backupOperationStatus is BackupOperationStatus.InProgress
                    SettingsItem(
                        title = stringResource(R.string.action_backup),
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.upload_24px),
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        },
                        trailingContent = if (isBackingUp) {
                            { CircularWavyProgressIndicator(modifier = Modifier.size(20.dp)) }
                        } else null,
                        enabled = !isBackingUp,
                        onClick = {
                            if (!isBackingUp) {
                                onAction(SettingsAction.LoadPreBackupCounts)
                                showCreateBackupSheet = true
                            }
                        }
                    )

                    val isRestoring = state.restoreState.isLoading || state.restoreOperationStatus is BackupOperationStatus.InProgress
                    SettingsItem(
                        title = stringResource(R.string.action_restore),
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.download_24px),
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
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

                    SettingsItem(
                        title = stringResource(R.string.settings_backup),
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.rule_settings),
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        },
                        onClick = {
                            showSettingsBackupSheet = true
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.auto_backup),
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.settings_backup_restore),
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                VerticalDivider(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .padding(start = 6.dp, end = 14.dp)
                                        .clip(CircleShape),
                                    thickness = 2.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                                )
                                Switch(
                                    checked = state.isAutoBackupEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            if (state.autoBackupFolderUri == null) {
                                                autoBackupFolderLauncher.launch(null)
                                            } else {
                                                onAction(SettingsAction.OnAutoBackupToggle(true))
                                            }
                                        } else {
                                            onAction(SettingsAction.OnAutoBackupToggle(false))
                                        }
                                    }
                                )
                            }
                        },
                        onClick = {
                            showAutoBackupOptionsSheet = true
                        }
                    )
                }
            }
            item {
                SettingSection(
                    title = stringResource(R.string.data_portability)
                ) {
                    val isExportingMarkdown = state.exportMarkdownState.isLoading
                    SettingsItem(
                        title = stringResource(R.string.export_markdown_archive),
                        subtitle = if (isExportingMarkdown) stringResource(R.string.exporting) else null,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.file_save_24px),
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        },
                        trailingContent = if (isExportingMarkdown) {
                            { CircularWavyProgressIndicator(modifier = Modifier.size(20.dp)) }
                        } else null,
                        enabled = !isExportingMarkdown,
                        onClick = {
                            if (!isExportingMarkdown) {
                                onAction(SettingsAction.LoadPreBackupCounts)
                                showExportMarkdownSheet = true
                            }
                        }
                    )

                    val isImportingMarkdown = state.importMarkdownState.isLoading
                    SettingsItem(
                        title = stringResource(R.string.import_markdown),
                        subtitle = if (isImportingMarkdown) stringResource(R.string.restoring) else null,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.folder_open_24px),
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        },
                        trailingContent = if (isImportingMarkdown) {
                            { CircularWavyProgressIndicator(modifier = Modifier.size(20.dp)) }
                        } else null,
                        enabled = !isImportingMarkdown,
                        onClick = {
                            if (!isImportingMarkdown) {
                                importMarkdownLauncher.launch(
                                    arrayOf(
                                        "text/markdown",
                                        "text/x-markdown",
                                        "text/plain",
                                        "application/zip",
                                        "*/*"
                                    )
                                )
                            }
                        }
                    )

                    val isImportingApp = state.appImportState.isLoading
                    SettingsItem(
                        title = stringResource(R.string.import_other_apps),
                        subtitle = if (isImportingApp) stringResource(R.string.restoring) else null,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.book_5_24px),
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        },
                        trailingContent = if (isImportingApp) {
                            { CircularWavyProgressIndicator(modifier = Modifier.size(20.dp)) }
                        } else null,
                        enabled = !isImportingApp,
                        onClick = {
                            if (!isImportingApp) {
                                showAppImportSheet = true
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

    if (showCreateBackupSheet) {
        CreateBackupBottomSheet(
            onDismiss = { showCreateBackupSheet = false },
            counts = state.preBackupCounts,
            onBackup = { includeMedia, includeAudios, includeSongs, includeSettings, includeMapKeys ->
                onAction(
                    SettingsAction.OnExportJournals(
                        includeMedia = includeMedia,
                        includeAudios = includeAudios,
                        includeSongFiles = includeSongs,
                        includeSettings = includeSettings,
                        includeMapKeys = includeMapKeys
                    )
                )
            }
        )
    }

    if (showAutoBackupOptionsSheet) {
        AutoBackupOptionsBottomSheet(
            onDismiss = { showAutoBackupOptionsSheet = false },
            folderName = state.autoBackupFolderName,
            onSelectFolder = {
                autoBackupFolderLauncher.launch(null)
            },
            onClearFolder = {
                onAction(SettingsAction.OnAutoBackupFolderCleared)
            },
            interval = state.autoBackupInterval,
            retentionCount = state.autoBackupRetentionCount,
            requireCharging = state.autoBackupRequireCharging,
            includeMedia = state.autoBackupIncludeMedia,
            includeAudios = state.autoBackupIncludeAudios,
            includeSongs = state.autoBackupIncludeSongs,
            includeSettings = state.autoBackupIncludeSettings,
            onTriggerBackupNow = {
                onAction(SettingsAction.OnTriggerAutoBackupNow)
            },
            onSave = { interval, retention, charging, media, audios, songs, settings ->
                if (interval != state.autoBackupInterval) {
                    onAction(SettingsAction.OnAutoBackupIntervalChange(interval))
                }
                if (retention != state.autoBackupRetentionCount) {
                    onAction(SettingsAction.OnAutoBackupRetentionChange(retention))
                }
                if (charging != state.autoBackupRequireCharging) {
                    onAction(SettingsAction.OnAutoBackupRequireChargingToggle(charging))
                }
                if (media != state.autoBackupIncludeMedia) {
                    onAction(SettingsAction.OnAutoBackupIncludeMediaToggle(media))
                }
                if (audios != state.autoBackupIncludeAudios) {
                    onAction(SettingsAction.OnAutoBackupIncludeAudiosToggle(audios))
                }
                if (songs != state.autoBackupIncludeSongs) {
                    onAction(SettingsAction.OnAutoBackupIncludeSongsToggle(songs))
                }
                if (settings != state.autoBackupIncludeSettings) {
                    onAction(SettingsAction.OnAutoBackupIncludeSettingsToggle(settings))
                }
            }
        )
    }

    if (showSettingsBackupSheet) {
        SettingsBackupBottomSheet(
            onDismiss = { showSettingsBackupSheet = false },
            hasMapKeys = state.hasMapKeys,
            onSaveFile = { includeMapKeys ->
                coroutineScope.launch {
                    settingsVM.exportSettingsJson(includeMapKeys).onSuccess { json ->
                        pendingSettingsJsonToSave = json
                        saveSettingsLauncher.launch("June_Settings_${System.currentTimeMillis()}.json")
                    }.onFailure {
                        Toast.makeText(context, failedToSaveMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRestoreFile = {
                restoreSettingsFileLauncher.launch(
                    arrayOf(
                        "application/json",
                        "text/plain",
                        "*/*"
                    )
                )
            }
        )
    }

    if (showExportMarkdownSheet) {
        ExportMarkdownBottomSheet(
            onDismiss = { showExportMarkdownSheet = false },
            counts = state.preBackupCounts,
            onExport = { includeMedia, includeAudios, includeSongs ->
                onAction(SettingsAction.OnExportMarkdown(includeMedia, includeAudios, includeSongs))
            }
        )
    }

    if (showAppImportSheet) {
        AppImportSheet(
            onDismiss = { showAppImportSheet = false },
            onSelectFile = {
                appImportLauncher.launch(
                    arrayOf(
                        "application/zip",
                        "application/json",
                        "application/octet-stream",
                        "*/*"
                    )
                )
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
                ) { Text(stringResource(R.string.action_restore)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreWarning = null }) { Text(stringResource(R.string.action_cancel)) }
            },
            text = {
                Text(stringResource(R.string.restore_warning_message))
            }
        )
    }

    if (pendingSettingsJsonToRestore != null) {
        JuneDialog(
            onDismissRequest = { pendingSettingsJsonToRestore = null },
            title = stringResource(R.string.restore_settings_title),
            icon = R.drawable.settings_24px,
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingSettingsJsonToRestore!!
                        pendingSettingsJsonToRestore = null
                        onAction(SettingsAction.OnRestoreSettings(json))
                    }
                ) { Text(stringResource(R.string.action_restore)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingSettingsJsonToRestore = null }) { Text(stringResource(R.string.action_cancel)) }
            },
            text = {
                Text(stringResource(R.string.restore_settings_desc))
            }
        )
    }
}
