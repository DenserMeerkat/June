package com.denser.june.presentation.screens.settings.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncAnalysis
import com.denser.june.presentation.screens.settings.components.SettingsItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SyncAnalysisSection(
    analysis: SyncAnalysis?,
    isAnalyzing: Boolean,
    onViewDetails: (SyncCategory?) -> Unit,
    onRefresh: () -> Unit = {}
) {
    val journalChanges = (analysis?.pendingUploadsCount ?: 0) +
            (analysis?.pendingDownloadsCount ?: 0) +
            (analysis?.pendingDeletionsCount ?: 0)

    val mediaChanges = (analysis?.pendingMediaUploadsCount ?: 0) +
            (analysis?.pendingMediaDownloadsCount ?: 0)

    val audioChanges = (analysis?.pendingAudioUploadsCount ?: 0) +
            (analysis?.pendingAudioDownloadsCount ?: 0)

    val songChanges = (analysis?.pendingSongUploadsCount ?: 0) +
            (analysis?.pendingSongDownloadsCount ?: 0)

    val totalChanges = journalChanges + mediaChanges + audioChanges + songChanges

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SyncMatrixTable(
                        analysis = analysis,
                        onCategoryClick = { category ->
                            onViewDetails(category)
                        }
                    )
                }
            }

            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }

        val canNavigate = totalChanges > 0
        SettingsItem(
            title = when {
                totalChanges > 0 -> "$totalChanges pending"
                analysis == null -> "Checking..."
                else -> stringResource(R.string.all_in_sync)
            },
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            leadingContent = {
                Icon(
                    painter = painterResource(
                        when {
                            analysis == null && isAnalyzing -> R.drawable.sync_24px
                            totalChanges > 0 -> R.drawable.cloud_sync_24px
                            else -> R.drawable.cloud_done_24px
                        }
                    ),
                    contentDescription = null,
                    tint = when {
                        totalChanges > 0 -> MaterialTheme.colorScheme.primary
                        analysis == null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                FilledTonalIconButton(
                    onClick = onRefresh,
                    enabled = !isAnalyzing,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sync_24px),
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            onClick = if (canNavigate) {
                { onViewDetails(null) }
            } else null
        )
    }
}
