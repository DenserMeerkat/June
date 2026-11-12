package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.core.utils.toFullDateTime
import com.denser.june.presentation.components.SyncIndicator
import com.denser.june.core.R

@Composable
fun SyncStatusCard(
    modifier: Modifier = Modifier,
    status: SyncStatus,
    lastSyncTime: Long,
    isVisible: Boolean,
    is24Hour: Boolean = false,
    horizontalPadding: Dp = 16.dp,
    cornerRadius: Dp = 16.dp
) {
    val clipboardManager = LocalClipboardManager.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SyncIndicator(
                    status = status,
                    size = 28.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val headerLabel = if (lastSyncTime > 0) {
                        stringResource(R.string.last_successful_sync)
                    } else {
                        stringResource(R.string.cloud_sync)
                    }

                    val valueText = if (lastSyncTime > 0) {
                        lastSyncTime.toFullDateTime(is24Hour = is24Hour)
                    } else {
                        stringResource(R.string.never_synced)
                    }

                    Text(
                        text = headerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    when (status) {
                        is SyncStatus.Preparing -> {
                            Text(
                                text = "Analyzing cloud differences...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        is SyncStatus.Syncing -> {
                            val currentOp = status.currentOperation
                            val uploadCount = status.uploadCount
                            val downloadCount = status.downloadCount

                            val syncText = when {
                                currentOp.contains("clean", ignoreCase = true) ||
                                        currentOp.contains("purge", ignoreCase = true) ||
                                        currentOp.contains("tombstone", ignoreCase = true) -> "Finalizing sync..."

                                currentOp.startsWith("Pushing", ignoreCase = true) ||
                                        currentOp.startsWith("Uploading", ignoreCase = true) -> {
                                    val targetItem = currentOp.removePrefix("Pushing ")
                                        .removePrefix("Uploading ")
                                        .removeSuffix("...")
                                        .trim()
                                    val displayItem = when {
                                        targetItem.equals("changes", ignoreCase = true) || targetItem.contains("journal", ignoreCase = true) -> "journal changes"
                                        targetItem.contains("media", ignoreCase = true) -> "media"
                                        targetItem.contains("audio", ignoreCase = true) -> "audio"
                                        targetItem.contains("art", ignoreCase = true) -> "album art"
                                        targetItem.contains("song", ignoreCase = true) -> "songs"
                                        else -> targetItem.ifBlank { "changes" }
                                    }
                                    "Uploading $displayItem..."
                                }

                                currentOp.startsWith("Downloading", ignoreCase = true) ||
                                        currentOp.startsWith("Pulling", ignoreCase = true) -> {
                                    val targetItem = currentOp.removePrefix("Downloading ")
                                        .removePrefix("Pulling ")
                                        .removeSuffix("...")
                                        .trim()
                                    val displayItem = when {
                                        targetItem.contains("journal", ignoreCase = true) -> "journal updates"
                                        targetItem.contains("media", ignoreCase = true) -> "media"
                                        targetItem.contains("audio", ignoreCase = true) -> "audio"
                                        targetItem.contains("art", ignoreCase = true) -> "album art"
                                        targetItem.contains("song", ignoreCase = true) -> "songs"
                                        else -> targetItem.ifBlank { "updates" }
                                    }
                                    "Downloading $displayItem..."
                                }

                                currentOp.contains("conflict", ignoreCase = true) -> "Resolving conflict..."
                                currentOp.contains("resurrect", ignoreCase = true) -> "Restoring remote entry..."
                                uploadCount > 0 && downloadCount > 0 -> "Pulled $downloadCount, pushed $uploadCount"
                                uploadCount > 0 -> "Pushed $uploadCount changes"
                                downloadCount > 0 -> "Pulled $downloadCount changes"
                                else -> currentOp.ifBlank { "Syncing..." }
                            }

                            Text(
                                text = syncText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        is SyncStatus.Success -> {
                            Text(
                                text = "All journals are up to date",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        is SyncStatus.Error -> {
                            val userFriendlyMsg = when {
                                status.message.contains("401", ignoreCase = true) || status.message.contains("403", ignoreCase = true) ->
                                    "Authentication failed — check server credentials"
                                status.message.contains("newer version", ignoreCase = true) ->
                                    "Cloud data was synced by a newer app version"
                                status.message.contains("disabled", ignoreCase = true) ->
                                    "Cloud sync is currently disabled"
                                status.message.contains("Connection", ignoreCase = true) || status.message.contains("Host", ignoreCase = true) ->
                                    "Unable to connect to cloud server"
                                else -> status.message
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Error: $userFriendlyMsg",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(status.message)) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.content_copy_24px),
                                        contentDescription = "Copy Error",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        is SyncStatus.Dirty -> {
                            Text(
                                text = "Unsynced changes detected",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        is SyncStatus.Idle -> {
                            Text(
                                text = if (lastSyncTime > 0) "All journals are up to date" else "Tap to sync now",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
