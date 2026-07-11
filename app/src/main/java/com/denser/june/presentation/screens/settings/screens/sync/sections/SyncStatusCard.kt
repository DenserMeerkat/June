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
        visible = isVisible && (lastSyncTime > 0 || status !is SyncStatus.Idle),
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
                    if (lastSyncTime > 0) {
                        Text(
                            text = "Last successful sync",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastSyncTime.toFullDateTime(is24Hour = is24Hour),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    when (status) {
                        is SyncStatus.Preparing -> {
                            Text(
                                "Analyzing cloud differences...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is SyncStatus.Syncing -> {
                            val currentOp = status.currentOperation
                            val uploadCount = status.uploadCount
                            val downloadCount = status.downloadCount
                            val total = status.totalOperations
                            val completed = uploadCount + downloadCount

                            val (header, subtitle) = when {
                                total <= 0 -> {
                                    "Syncing" to currentOp.ifBlank { "Initializing connection..." }
                                }

                                currentOp.contains("clean", ignoreCase = true) ||
                                        currentOp.contains("purge", ignoreCase = true) ||
                                        currentOp.contains("tombstone", ignoreCase = true) -> {
                                    "Syncing (Finalizing)" to currentOp
                                }

                                currentOp.startsWith("Pushing", ignoreCase = true) ||
                                        currentOp.startsWith("Uploading", ignoreCase = true) -> {
                                    val targetItem = currentOp.removePrefix("Pushing ")
                                        .removePrefix("Uploading ")
                                        .removeSuffix("...")
                                    val displayItem = if (targetItem.equals("changes", ignoreCase = true)) {
                                        "untitled journal"
                                    } else {
                                        targetItem
                                    }
                                    val progressHeader = "Pushing changes (${completed + 1} of $total)"
                                    val progressDetail = "Uploading $displayItem..."
                                    progressHeader to progressDetail
                                }

                                currentOp.startsWith("Downloading", ignoreCase = true) ||
                                        currentOp.startsWith("Pulling", ignoreCase = true) -> {
                                    val progressHeader = "Pulling changes (${completed + 1} of $total)"
                                    val progressDetail = "Downloading journal updates..."
                                    progressHeader to progressDetail
                                }

                                uploadCount > 0 && downloadCount > 0 -> {
                                    "Syncing ($completed of $total)" to "Pulled $downloadCount, pushed $uploadCount"
                                }

                                else -> {
                                    "Syncing (${completed + 1} of $total)" to currentOp.ifBlank { "Processing updates..." }
                                }
                            }

                            Column {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is SyncStatus.Success -> {
                            Text(
                                "All journals are up to date",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        is SyncStatus.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Error: ${status.message}",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(status.message)) },
                                    modifier = Modifier.size(32.dp)
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
                                "Unsynced changes detected",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
