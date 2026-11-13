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
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBackupBottomSheet(
    onDismiss: () -> Unit,
    onBackup: (includeMedia: Boolean, includeAudios: Boolean, includeSongs: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var includeMedia by remember { mutableStateOf(true) }
    var includeAudios by remember { mutableStateOf(true) }
    var includeSongs by remember { mutableStateOf(true) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null
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
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.create_backup_dialog_title),
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

            JuneConnectedList(
                spacing = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                JuneConnectedItem(
                    title = stringResource(R.string.media),
                    index = 0,
                    count = 3,
                    containerColor = mediaContainerColor,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    trailingContent = {
                        Checkbox(
                            checked = includeMedia,
                            onCheckedChange = null
                        )
                    },
                    onClick = { includeMedia = !includeMedia }
                )
                JuneConnectedItem(
                    title = stringResource(R.string.audios),
                    index = 1,
                    count = 3,
                    containerColor = audiosContainerColor,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    trailingContent = {
                        Checkbox(
                            checked = includeAudios,
                            onCheckedChange = null
                        )
                    },
                    onClick = { includeAudios = !includeAudios }
                )
                JuneConnectedItem(
                    title = stringResource(R.string.songs),
                    index = 2,
                    count = 3,
                    containerColor = songsContainerColor,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    trailingContent = {
                        Checkbox(
                            checked = includeSongs,
                            onCheckedChange = null
                        )
                    },
                    onClick = { includeSongs = !includeSongs }
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
                        onBackup(includeMedia, includeAudios, includeSongs)
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
    onExport: (includeMedia: Boolean, includeAudios: Boolean, includeSongs: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var includeMedia by remember { mutableStateOf(true) }
    var includeAudios by remember { mutableStateOf(true) }
    var includeSongs by remember { mutableStateOf(true) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null
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
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                JuneConnectedItem(
                    title = stringResource(R.string.media),
                    index = 0,
                    count = 3,
                    containerColor = mediaContainerColor,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    trailingContent = {
                        Checkbox(
                            checked = includeMedia,
                            onCheckedChange = null
                        )
                    },
                    onClick = { includeMedia = !includeMedia }
                )
                JuneConnectedItem(
                    title = stringResource(R.string.audios),
                    index = 1,
                    count = 3,
                    containerColor = audiosContainerColor,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    trailingContent = {
                        Checkbox(
                            checked = includeAudios,
                            onCheckedChange = null
                        )
                    },
                    onClick = { includeAudios = !includeAudios }
                )
                JuneConnectedItem(
                    title = stringResource(R.string.songs),
                    index = 2,
                    count = 3,
                    containerColor = songsContainerColor,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    trailingContent = {
                        Checkbox(
                            checked = includeSongs,
                            onCheckedChange = null
                        )
                    },
                    onClick = { includeSongs = !includeSongs }
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
