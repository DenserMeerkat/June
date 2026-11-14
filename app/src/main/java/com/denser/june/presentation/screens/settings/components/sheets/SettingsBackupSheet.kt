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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupBottomSheet(
    onDismiss: () -> Unit,
    hasMapKeys: Boolean,
    onSaveFile: (includeMapKeys: Boolean) -> Unit,
    onRestoreFile: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                    text = stringResource(R.string.settings_backup),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            val mapKeysColor by animateColorAsState(
                targetValue = if (includeMapKeys) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                label = "settingsBackupMapKeysColor"
            )

            JuneConnectedList(
                spacing = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                JuneCheckboxTile(
                    checked = includeMapKeys,
                    title = stringResource(R.string.include_api_keys),
                    onCheckedChange = { includeMapKeys = it },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.warning_24px),
                            contentDescription = null,
                            tint = if (hasMapKeys) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    enabled = hasMapKeys,
                    index = 0,
                    count = 1,
                    containerColor = mapKeysColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            JuneFloatingActionBar(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                JuneFloatingAction(
                    onClick = {
                        onDismiss()
                        onRestoreFile()
                    },
                    label = stringResource(R.string.action_restore),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.download_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                JuneFloatingAction(
                    onClick = {
                        onDismiss()
                        onSaveFile(includeMapKeys)
                    },
                    label = stringResource(R.string.action_save),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.file_save_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}
