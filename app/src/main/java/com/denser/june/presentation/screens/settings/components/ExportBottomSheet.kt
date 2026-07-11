package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar

enum class ExportFormat {
    JSON, MARKDOWN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    onDismiss: () -> Unit,
    onExport: (format: ExportFormat, includeMedia: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var exportFormat by remember { mutableStateOf(ExportFormat.JSON) }
    var includeMedia by remember { mutableStateOf(true) }

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
                    text = "Export Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = "Select format and options for exporting your journal entries:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsItem(
                    title = "JSON (Backup)",
                    subtitle = "Full database backup. Best for restoring data.",
                    leadingContent = {
                        RadioButton(
                            selected = exportFormat == ExportFormat.JSON,
                            onClick = { exportFormat = ExportFormat.JSON }
                        )
                    },
                    containerColor = if (exportFormat == ExportFormat.JSON) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                    onClick = { exportFormat = ExportFormat.JSON }
                )

                SettingsItem(
                    title = "Markdown",
                    subtitle = "Plain text files. Ideal for offline reading and editing.",
                    leadingContent = {
                        RadioButton(
                            selected = exportFormat == ExportFormat.MARKDOWN,
                            onClick = { exportFormat = ExportFormat.MARKDOWN }
                        )
                    },
                    containerColor = if (exportFormat == ExportFormat.MARKDOWN) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                    onClick = { exportFormat = ExportFormat.MARKDOWN }
                )
            }

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
                text = if (includeMedia) "Export size might be large. Photos & videos will be included." else "Photos & videos will not be saved. Faster and smaller.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            JuneFloatingActionBar(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                JuneFloatingAction(
                    onClick = onDismiss,
                    label = "Cancel",
                    icon = { Icon(painterResource(R.drawable.close_24px), contentDescription = null) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                JuneFloatingAction(
                    onClick = {
                        onDismiss()
                        onExport(exportFormat, includeMedia)
                    },
                    label = "Export",
                    icon = { Icon(painterResource(R.drawable.check_24px), contentDescription = null) }
                )
            }
        }
    }
}
