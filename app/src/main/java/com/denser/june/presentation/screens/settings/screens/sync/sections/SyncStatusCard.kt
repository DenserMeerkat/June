package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.core.utils.toFullDateTime
import com.denser.june.presentation.components.SyncIndicator

@Composable
fun SyncStatusCard(
    status: SyncStatus,
    lastSyncTime: Long,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    cornerRadius: Dp = 16.dp
) {
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

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (lastSyncTime > 0) {
                        Text(
                            text = "Last successful sync",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastSyncTime.toFullDateTime(),
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
                            Column {
                                Text(
                                    text = if (status.totalOperations > 0) {
                                        "Merging changes (${status.downloadCount} pulled, ${status.uploadCount} pushed)"
                                    } else "Syncing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (status.currentOperation.isNotBlank()) {
                                    Text(
                                        text = status.currentOperation,
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
                            Text(
                                "Error: ${status.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
