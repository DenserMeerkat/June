package com.denser.june.presentation.screens.settings.components.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList
import com.denser.june.presentation.components.JuneCheckboxTile
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar

import com.denser.june.core.domain.backup.PreBackupCounts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBackupBottomSheet(
    onDismiss: () -> Unit,
    counts: PreBackupCounts? = null,
    onBackup: (includeMedia: Boolean, includeAudios: Boolean, includeSongs: Boolean, includeSettings: Boolean, includeMapKeys: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var includeMedia by remember { mutableStateOf(true) }
    var includeAudios by remember { mutableStateOf(true) }
    var includeSongs by remember { mutableStateOf(true) }
    var includeSettings by remember { mutableStateOf(true) }
    var includeMapKeys by remember { mutableStateOf(false) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.action_backup),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.create_backup_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.include),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val mediaContainerColor by animateColorAsState(
                targetValue = if (includeMedia) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupMediaColor"
            )
            val audiosContainerColor by animateColorAsState(
                targetValue = if (includeAudios) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupAudiosColor"
            )
            val songsContainerColor by animateColorAsState(
                targetValue = if (includeSongs) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupSongsColor"
            )
            val settingsContainerColor by animateColorAsState(
                targetValue = if (includeSettings) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupSettingsColor"
            )
            val mapKeysContainerColor by animateColorAsState(
                targetValue = if (includeMapKeys) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupMapKeysColor"
            )

            JuneConnectedList(
                spacing = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val mediaTitle = counts?.let { "${it.totalMedia} " }.orEmpty() + stringResource(R.string.media)
                val audiosTitle = counts?.let { "${it.totalAudios} " }.orEmpty() + stringResource(R.string.audios)
                val songsTitle = counts?.let { "${it.totalSongs} " }.orEmpty() + stringResource(R.string.songs)

                JuneCheckboxTile(
                    checked = includeMedia,
                    title = mediaTitle,
                    onCheckedChange = { includeMedia = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.photo_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    index = 0,
                    count = 3,
                    containerColor = mediaContainerColor
                )
                JuneCheckboxTile(
                    checked = includeAudios,
                    title = audiosTitle,
                    onCheckedChange = { includeAudios = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.mic_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    index = 1,
                    count = 3,
                    containerColor = audiosContainerColor
                )
                JuneCheckboxTile(
                    checked = includeSongs,
                    title = songsTitle,
                    onCheckedChange = { includeSongs = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.music_note_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    index = 2,
                    count = 3,
                    containerColor = songsContainerColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hasMapKeys = counts?.hasMapKeys == true
            val isMapKeysEnabled = includeSettings && hasMapKeys

            JuneConnectedList(
                spacing = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                JuneCheckboxTile(
                    checked = includeSettings,
                    title = stringResource(R.string.include_settings),
                    onCheckedChange = { next ->
                        includeSettings = next
                        if (!next) {
                            includeMapKeys = false
                        }
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.rule_settings),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    index = 0,
                    count = 2,
                    containerColor = settingsContainerColor
                )
                JuneCheckboxTile(
                    checked = includeMapKeys,
                    title = stringResource(R.string.include_api_keys),
                    onCheckedChange = { includeMapKeys = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.warning_24px),
                            contentDescription = null,
                            tint = if (isMapKeysEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    enabled = isMapKeysEnabled,
                    index = 1,
                    count = 2,
                    containerColor = mapKeysContainerColor
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            JuneFloatingActionBar(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                JuneFloatingAction(
                    onClick = onDismiss,
                    label = stringResource(R.string.action_cancel),
                    icon = { Icon(painterResource(R.drawable.close_24px), contentDescription = null) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                JuneFloatingAction(
                    onClick = {
                        onDismiss()
                        onBackup(includeMedia, includeAudios, includeSongs, includeSettings, includeMapKeys)
                    },
                    label = stringResource(R.string.action_backup),
                    icon = { Icon(painterResource(R.drawable.check_24px), contentDescription = null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportMarkdownBottomSheet(
    onDismiss: () -> Unit,
    counts: PreBackupCounts?,
    onExport: (includeMedia: Boolean, includeAudios: Boolean, includeSongs: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var includeMedia by remember { mutableStateOf(true) }
    var includeAudios by remember { mutableStateOf(true) }
    var includeSongs by remember { mutableStateOf(true) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.export_markdown_archive),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.export_markdown_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.include),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val mediaContainerColor by animateColorAsState(
                targetValue = if (includeMedia) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupMediaColor"
            )
            val audiosContainerColor by animateColorAsState(
                targetValue = if (includeAudios) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupAudiosColor"
            )
            val songsContainerColor by animateColorAsState(
                targetValue = if (includeSongs) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "backupSongsColor"
            )

            JuneConnectedList(
                spacing = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val mediaTitle = counts?.let { "${it.totalMedia} " }.orEmpty() + stringResource(R.string.media)
                val audiosTitle = counts?.let { "${it.totalAudios} " }.orEmpty() + stringResource(R.string.audios)
                val songsTitle = counts?.let { "${it.totalSongs} " }.orEmpty() + stringResource(R.string.songs)

                JuneCheckboxTile(
                    checked = includeMedia,
                    title = mediaTitle,
                    onCheckedChange = { includeMedia = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.photo_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    index = 0,
                    count = 3,
                    containerColor = mediaContainerColor
                )
                JuneCheckboxTile(
                    checked = includeAudios,
                    title = audiosTitle,
                    onCheckedChange = { includeAudios = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.mic_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    index = 1,
                    count = 3,
                    containerColor = audiosContainerColor
                )
                JuneCheckboxTile(
                    checked = includeSongs,
                    title = songsTitle,
                    onCheckedChange = { includeSongs = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.music_note_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    index = 2,
                    count = 3,
                    containerColor = songsContainerColor
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            JuneFloatingActionBar(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                JuneFloatingAction(
                    onClick = onDismiss,
                    label = stringResource(R.string.action_cancel),
                    icon = { Icon(painterResource(R.drawable.close_24px), contentDescription = null) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                JuneFloatingAction(
                    onClick = {
                        onDismiss()
                        onExport(includeMedia, includeAudios, includeSongs)
                    },
                    label = stringResource(R.string.action_export),
                    icon = { Icon(painterResource(R.drawable.check_24px), contentDescription = null) }
                )
            }
        }
    }
}
