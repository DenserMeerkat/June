package com.denser.june.presentation.components.lightbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.media.exif.model.MediaExifInfo
import com.denser.june.core.utils.toExifDateTimeDisplay
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedItemShape
import com.denser.june.presentation.components.JuneConnectedList
import java.io.File

import com.denser.june.presentation.screens.home.timeline.components.TimelineJournalCard

@Composable
fun LightboxInfoSheetContent(
    exifInfo: MediaExifInfo?,
    dateTime: Long?,
    parentJournal: Journal?,
    isJournalNavigationEnabled: Boolean,
    isMediaBackedUp: Boolean,
    isPendingSync: Boolean,
    syncProvider: String?,
    isCompressing: Boolean,
    canCompressFurther: Boolean,
    allowCompression: Boolean = true,
    onJournalClick: () -> Unit,
    onCompressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }

        Text(
            text = exifInfo?.dateTaken
                ?: (dateTime ?: parentJournal?.dateTime ?: System.currentTimeMillis()).toExifDateTimeDisplay(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isJournalNavigationEnabled && parentJournal != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TimelineJournalCard(
                journal = parentJournal,
                onJournalClick = onJournalClick
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        LightboxDetailsSection(
            exifInfo = exifInfo,
            isBackedUp = isMediaBackedUp,
            isPendingSync = isPendingSync,
            syncProvider = syncProvider,
            isCompressing = isCompressing,
            canCompressFurther = canCompressFurther,
            allowCompression = allowCompression,
            onCompressClick = onCompressClick
        )
    }
}

@Composable
fun LightboxDetailsSection(
    exifInfo: MediaExifInfo?,
    isBackedUp: Boolean,
    isPendingSync: Boolean,
    syncProvider: String? = null,
    isCompressing: Boolean,
    canCompressFurther: Boolean = false,
    allowCompression: Boolean = true,
    onCompressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backupSubtitle = when {
        isBackedUp -> when (syncProvider) {
            "GoogleDrive" -> "Synced with Google Drive"
            "WebDAV" -> "Synced with WebDAV"
            else -> "Synced with cloud"
        }
        isPendingSync -> "Waiting to sync"
        else -> "Stored locally"
    }

    JuneConnectedList(
        modifier = modifier.fillMaxWidth()
    ) {
        JuneConnectedItem(
            title = when {
                isBackedUp -> "Backed up"
                isPendingSync -> "Pending backup"
                else -> "Not backed up"
            },
            subtitle = backupSubtitle,
            leadingContent = {
                Icon(
                    painter = painterResource(
                        if (isBackedUp) R.drawable.cloud_done_24px
                        else if (isPendingSync) R.drawable.cloud_upload_24px
                        else R.drawable.cloud_off_24px
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        )

        if (allowCompression) {
            JuneConnectedItem(
                title = if (canCompressFurther) "Compress" else "Optimized",
                enabled = !isCompressing && canCompressFurther,
                onClick = onCompressClick,
                leadingContent = {
                    Icon(
                        painter = painterResource(
                            if (canCompressFurther) R.drawable.compress_24px
                            else R.drawable.check_circle_24px
                        ),
                        contentDescription = null,
                        tint = if (canCompressFurther) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = if (isCompressing) {
                    {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )
        }

        if (exifInfo != null) {
            JuneConnectedItem(
                title = exifInfo.deviceName ?: if (exifInfo.isVideo) "Video Details" else "Photo Details",
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.camera_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            if (!exifInfo.isVideo) {
                val f = exifInfo.aperture?.let { if (it.startsWith("f/")) "ƒ/${it.removePrefix("f/")}" else it }
                val specs = listOfNotNull(f, exifInfo.shutterSpeed, exifInfo.focalLength, exifInfo.iso)

                if (specs.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(JuneConnectedItemShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            specs.forEachIndexed { index, spec ->
                                Text(
                                    text = spec,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (index < specs.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .height(6.dp)
                                            .width(1.5.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(JuneConnectedItemShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = exifInfo.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val chips = buildList {
                        if (exifInfo.isVideo) {
                            exifInfo.qualityLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
                            exifInfo.resolution?.takeIf { it.isNotBlank() }?.let { add(it) }
                            if (exifInfo.isHdr) {
                                add(exifInfo.hdrFormat ?: "HLG")
                            }
                        } else {
                            exifInfo.megapixels?.takeIf { it.isNotBlank() }?.let { add(it) }
                            exifInfo.resolution?.takeIf { it.isNotBlank() }?.let { add(it) }
                            if (exifInfo.isHdr) {
                                add(exifInfo.hdrFormat ?: "HDR")
                            }
                        }
                    }

                    if (chips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            chips.forEach { chip ->
                                SpecChip(chip)
                            }
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(JuneConnectedItemShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = exifInfo.fileSizeFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val dirPath = File(exifInfo.filePath).parent ?: exifInfo.filePath
                    Text(
                        text = dirPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
