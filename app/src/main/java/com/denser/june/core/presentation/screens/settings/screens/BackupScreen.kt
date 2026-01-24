package com.denser.june.core.presentation.screens.settings.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.backup.ExportState
import com.denser.june.core.domain.backup.RestoreState
import com.denser.june.core.navigation.AppNavigator
import com.denser.june.core.presentation.components.JuneAppBarType
import com.denser.june.core.presentation.components.JuneTopAppBar
import com.denser.june.core.presentation.screens.settings.SettingsAction
import com.denser.june.core.presentation.screens.settings.SettingsState
import org.koin.compose.koinInject

import com.denser.june.R
import com.denser.june.core.domain.backup.RestoreFailedException
import com.denser.june.core.presentation.screens.settings.section.SettingSection
import com.denser.june.core.presentation.screens.settings.section.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showExportDialog by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf<String?>(null) }

    var includeMedia by remember { mutableStateOf(true) }

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

    BackHandler { navigator.navigateBack() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(painterResource(R.drawable.arrow_back_24px), "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Securely export your data or restore from a previous backup",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                SettingSection {
                    SettingsItem(
                        title = "Export Data",
                        subtitle = "Save your journals and media to a secure file.",
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.upload_24px),
                                null
                            )
                        }
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (state.exportState !is ExportState.Exporting) {
                                    showExportDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.exportState !is ExportState.Exporting
                        ) {
                            if (state.exportState is ExportState.Exporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating Backup...")
                            } else {
                                Icon(
                                    painterResource(R.drawable.save_24px),
                                    null,
                                    Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Backup")
                            }
                        }
                    }

                    SettingsItem(
                        title = "Restore Data",
                        subtitle = "Import data from a previously saved backup file.",
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.download_24px),
                                null
                            )
                        }
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/json",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.restoreState !is RestoreState.Restoring
                        ) {
                            if (state.restoreState is RestoreState.Restoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restoring...")
                            } else {
                                Icon(
                                    painterResource(R.drawable.folder_open_24px),
                                    null,
                                    Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Backup File")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(painterResource(R.drawable.upload_24px), null) },
            title = { Text("Create Backup?") },
            text = {
                Column {
                    Text("This will create a ZIP file containing your journal entries.")

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Include Media",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = includeMedia,
                            onCheckedChange = { includeMedia = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (includeMedia) "Backup size might be large. Photos & videos will be included." else "Photos & videos will not be saved. Faster and smaller.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        onAction(SettingsAction.OnExportJournals(includeMedia))
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRestoreWarning != null) {
        AlertDialog(
            onDismissRequest = { showRestoreWarning = null },
            icon = { Icon(painterResource(R.drawable.cloud_sync_24px), null) },
            title = { Text("Restore Backup?") },
            text = {
                Text("This will merge the backup with your current data.\n\n• Entries with matching IDs will be OVERWRITTEN.\n• New entries will be ADDED.\n\nThis action cannot be undone.")
            },
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
            }
        )
    }
}