package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.presentation.components.JuneTextField

@Composable
fun WebDavConfigSection(
    isVisible: Boolean,
    webDavUrl: String,
    webDavUser: String,
    webDavPass: String,
    isConfigLocked: Boolean,
    status: SyncStatus,
    urlError: String? = null,
    userError: String? = null,
    passError: String? = null,
    onUrlChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onToggleLock: () -> Unit
) {
    var passVisible by remember { mutableStateOf(false) }
    val isSyncing = status is SyncStatus.Syncing || status is SyncStatus.Preparing

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.webdav_configuration),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Switch(
                        checked = isConfigLocked,
                        onCheckedChange = { onToggleLock() },
                        modifier = Modifier.scale(0.75f),
                        thumbContent = {
                            Icon(
                                painterResource(if (isConfigLocked) R.drawable.lock_24px else R.drawable.lock_open_right_24px),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isConfigLocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }

                JuneTextField(
                    value = webDavUrl,
                    onValueChange = onUrlChange,
                    label = stringResource(R.string.server_url_label),
                    placeholder = stringResource(R.string.server_url_placeholder),
                    leadingIcon = R.drawable.link_24px,
                    keyboardType = KeyboardType.Uri,
                    enabled = !isSyncing && !isConfigLocked,
                    errorText = urlError
                )
                JuneTextField(
                    value = webDavUser,
                    onValueChange = onUserChange,
                    label = stringResource(R.string.username_label),
                    placeholder = stringResource(R.string.username_placeholder),
                    leadingIcon = R.drawable.person_24px,
                    enabled = !isSyncing && !isConfigLocked,
                    errorText = userError
                )
                JuneTextField(
                    value = webDavPass,
                    onValueChange = onPassChange,
                    label = stringResource(R.string.password_app_token_label),
                    placeholder = "••••••••••",
                    leadingIcon = R.drawable.lock_24px,
                    keyboardType = KeyboardType.Password,
                    enabled = !isSyncing && !isConfigLocked,
                    errorText = passError,
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { passVisible = !passVisible },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painterResource(if (passVisible) R.drawable.visibility_24px else R.drawable.visibility_off_24px),
                                contentDescription = if (passVisible) "Hide password" else "Show password",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}
