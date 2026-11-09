package com.denser.june.presentation.screens.settings.screens.sync.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncAnalysis

enum class SyncCategory(
    val titleRes: Int
) {
    JOURNALS(R.string.journals),
    MEDIA(R.string.media),
    AUDIOS(R.string.audios),
    SONGS(R.string.music)
}

data class SyncCategoryStats(
    val category: SyncCategory,
    val localCount: Int?,
    val remoteCount: Int?,
    val pendingUploads: Int,
    val pendingDownloads: Int,
    val pendingDeletions: Int
)

fun SyncAnalysis?.getStatsFor(category: SyncCategory): SyncCategoryStats {
    if (this == null) {
        return SyncCategoryStats(category, null, null, 0, 0, 0)
    }
    return when (category) {
        SyncCategory.JOURNALS -> SyncCategoryStats(
            category = category,
            localCount = localJournals,
            remoteCount = remoteJournals,
            pendingUploads = pendingUploadsCount,
            pendingDownloads = pendingDownloadsCount,
            pendingDeletions = pendingDeletionsCount
        )
        SyncCategory.MEDIA -> SyncCategoryStats(
            category = category,
            localCount = localMedia,
            remoteCount = remoteMedia,
            pendingUploads = pendingMediaUploadsCount,
            pendingDownloads = pendingMediaDownloadsCount,
            pendingDeletions = 0
        )
        SyncCategory.AUDIOS -> SyncCategoryStats(
            category = category,
            localCount = localAudioFiles,
            remoteCount = remoteAudioFiles,
            pendingUploads = pendingAudioUploadsCount,
            pendingDownloads = pendingAudioDownloadsCount,
            pendingDeletions = 0
        )
        SyncCategory.SONGS -> SyncCategoryStats(
            category = category,
            localCount = localSongFiles,
            remoteCount = remoteSongFiles,
            pendingUploads = pendingSongUploadsCount,
            pendingDownloads = pendingSongDownloadsCount,
            pendingDeletions = 0
        )
    }
}

@Composable
fun SyncTableHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.label_overview),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.35f)
        )

        Box(
            modifier = Modifier.weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.devices_24px),
                contentDescription = stringResource(R.string.local),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Box(
            modifier = Modifier.weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.cloud_24px),
                contentDescription = stringResource(R.string.cloud),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Box(
            modifier = Modifier.weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.cloud_upload_24px),
                contentDescription = "Uploads",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier.weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.cloud_download_24px),
                contentDescription = "Downloads",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        Box(
            modifier = Modifier.weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.delete_24px),
                contentDescription = "Deletions",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SyncTableRow(
    category: SyncCategory,
    stats: SyncCategoryStats,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    hasAnalysis: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1.35f)
            )

            Text(
                text = stats.localCount?.toString() ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.7f)
            )

            Text(
                text = stats.remoteCount?.toString() ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.7f)
            )

            Text(
                text = if (hasAnalysis && stats.pendingUploads > 0) "${stats.pendingUploads}" else "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (stats.pendingUploads > 0) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = if (stats.pendingUploads > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                },
                modifier = Modifier.weight(0.7f)
            )

            Text(
                text = if (hasAnalysis && stats.pendingDownloads > 0) "${stats.pendingDownloads}" else "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (stats.pendingDownloads > 0) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = if (stats.pendingDownloads > 0) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                },
                modifier = Modifier.weight(0.7f)
            )

            Text(
                text = if (hasAnalysis && stats.pendingDeletions > 0) "${stats.pendingDeletions}" else "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (stats.pendingDeletions > 0) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = if (stats.pendingDeletions > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                },
                modifier = Modifier.weight(0.7f)
            )
        }
    }
}

@Composable
fun SyncMatrixTable(
    modifier: Modifier = Modifier,
    analysis: SyncAnalysis?,
    selectedCategory: SyncCategory? = null,
    onCategoryClick: ((SyncCategory) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SyncTableHeader()

        SyncCategory.entries.forEach { category ->
            val stats = analysis.getStatsFor(category)
            val isSelected = selectedCategory == category
            SyncTableRow(
                category = category,
                stats = stats,
                isSelected = isSelected,
                hasAnalysis = analysis != null,
                onClick = onCategoryClick?.let { { it(category) } }
            )
        }
    }
}
