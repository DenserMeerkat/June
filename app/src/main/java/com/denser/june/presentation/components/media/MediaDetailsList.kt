package com.denser.june.presentation.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.denser.june.core.media.exif.model.AudioMetadata
import com.denser.june.core.media.exif.model.MediaMetadata
import com.denser.june.core.media.exif.model.PhotoMetadata
import com.denser.june.core.media.exif.model.VideoMetadata
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedItemShape
import com.denser.june.presentation.components.JuneConnectedList
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaDetailsList(
    metadata: MediaMetadata?,
    isBackedUp: Boolean,
    isPendingSync: Boolean,
    syncProvider: String? = null,
    isCompressing: Boolean = false,
    canCompressFurther: Boolean = false,
    allowCompression: Boolean = true,
    onCompressClick: () -> Unit = {},
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

        if (metadata != null) {
            val detailTitle = when (metadata) {
                is AudioMetadata -> metadata.deviceName ?: "Audio Details"
                is VideoMetadata -> metadata.deviceName ?: "Video Details"
                is PhotoMetadata -> metadata.deviceName ?: "Photo Details"
            }
            val detailIcon = when (metadata) {
                is AudioMetadata -> R.drawable.mic_24px
                else -> R.drawable.camera_24px
            }

            JuneConnectedItem(
                title = detailTitle,
                leadingContent = {
                    Icon(
                        painter = painterResource(detailIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            when (metadata) {
                is AudioMetadata -> {
                    val specs = listOfNotNull(
                        metadata.bitrate,
                        metadata.sampleRate,
                        metadata.channels,
                        metadata.audioFormat
                    )
                    if (specs.isNotEmpty()) {
                        MediaSpecsRow(specs = specs)
                    }
                }
                is PhotoMetadata -> {
                    val f = metadata.aperture?.let { if (it.startsWith("f/")) "ƒ/${it.removePrefix("f/")}" else it }
                    val specs = listOfNotNull(f, metadata.shutterSpeed, metadata.focalLength, metadata.iso)
                    if (specs.isNotEmpty()) {
                        MediaSpecsRow(specs = specs)
                    }
                }
                is VideoMetadata -> {}
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
                    val displayName = when (metadata) {
                        is AudioMetadata -> metadata.journalTitle?.takeIf { it.isNotBlank() } ?: metadata.fileName
                        else -> metadata.fileName
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val chips = buildList {
                        when (metadata) {
                            is AudioMetadata -> {
                                metadata.audioFormat?.takeIf { it.isNotBlank() }?.let { add(it) }
                                metadata.duration?.takeIf { it.isNotBlank() }?.let { add(it) }
                            }
                            is VideoMetadata -> {
                                metadata.qualityLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
                                metadata.resolution?.takeIf { it.isNotBlank() }?.let { add(it) }
                                if (metadata.isHdr) {
                                    add(metadata.hdrFormat ?: "HLG")
                                }
                            }
                            is PhotoMetadata -> {
                                metadata.megapixels?.takeIf { it.isNotBlank() }?.let { add(it) }
                                metadata.resolution?.takeIf { it.isNotBlank() }?.let { add(it) }
                                if (metadata.isHdr) {
                                    add(metadata.hdrFormat ?: "HDR")
                                }
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
                        text = metadata.fileSizeFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val dirPath = File(metadata.filePath).parent ?: metadata.filePath
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
private fun MediaSpecsRow(
    specs: List<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
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

