package com.denser.june.presentation.screens.settings.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.sync.SyncAnalysis
import com.denser.june.core.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SyncAnalysisSection(
    analysis: SyncAnalysis?,
    isAnalyzing: Boolean,
    onViewDetails: () -> Unit
) {
    if (analysis == null && !isAnalyzing) return

    val journalChanges = (analysis?.pendingUploadsCount ?: 0) +
            (analysis?.pendingDownloadsCount ?: 0) +
            (analysis?.pendingDeletionsCount ?: 0)

    val mediaChanges = (analysis?.pendingMediaUploadsCount ?: 0) +
            (analysis?.pendingMediaDownloadsCount ?: 0)

    val totalChanges = journalChanges + mediaChanges

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnalysisStatColumn(
                        "Local",
                        analysis?.localJournals,
                        analysis?.localMedia,
                        Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    AnalysisStatColumn(
                        "Cloud",
                        analysis?.remoteJournals,
                        analysis?.remoteMedia,
                        Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    when {
                                        totalChanges > 0 -> MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        )

                                        analysis == null -> MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.05f
                                        )

                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.1f
                                        )
                                    },
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(
                                    when {
                                        totalChanges > 0 -> R.drawable.sync_24px
                                        analysis == null -> R.drawable.track_changes_24px
                                        else -> R.drawable.cloud_done_24px
                                    }
                                ),
                                contentDescription = null,
                                tint = when {
                                    totalChanges > 0 -> MaterialTheme.colorScheme.primary
                                    analysis == null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                when {
                                    totalChanges > 0 -> "Sync required"
                                    analysis == null -> "Analysis pending"
                                    else -> "All caught up"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when {
                                    analysis == null -> "Check for differences"
                                    totalChanges == 0 -> "Local & cloud are matching"
                                    journalChanges > 0 && mediaChanges > 0 -> "$journalChanges journals & $mediaChanges media"
                                    journalChanges > 0 -> "$journalChanges journals pending"
                                    else -> "$mediaChanges media files pending"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (totalChanges > 0) {
                        FilledTonalButton(
                            onClick = onViewDetails,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Review", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        }
    }
}

@Composable
private fun AnalysisStatColumn(title: String, journals: Int?, media: Int?, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Text(
            "${journals ?: "-"} Journals",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "${media ?: "-"} Media",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
