package com.denser.june.presentation.components.lightbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.compress.model.CompressionPreviewResult
import com.denser.june.core.media.exif.model.MediaExifInfo
import com.denser.june.core.utils.formatBytes
import com.denser.june.core.utils.toMediumDate
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem

@Composable
fun LightboxTopBar(
    isCompressMode: Boolean,
    isInfoVisible: Boolean,
    isDarkTheme: Boolean,
    isGeneratingPreview: Boolean,
    isCompressing: Boolean,
    selectedPreset: CompressionPreset?,
    appliedPreset: CompressionPreset?,
    availablePresets: Set<CompressionPreset>,
    currentPath: String?,
    exifInfo: MediaExifInfo?,
    dateTime: Long?,
    parentJournal: Journal?,
    currentPreview: CompressionPreviewResult?,
    hasUncompressedOriginal: Boolean,
    uncompressedOriginalBytes: Long?,
    allowCompression: Boolean = true,
    onBackOrCancel: () -> Unit,
    onSaveCompression: () -> Unit,
    onOpenInfo: () -> Unit,
    onStartCompression: () -> Unit,
    onDeleteMedia: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isDarkTheme) Color.Black.copy(alpha = 0.5f)
                else Color.White.copy(alpha = 0.85f)
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledIconButton(
                onClick = onBackOrCancel,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                ),
            ) {
                Icon(
                    painter = painterResource(if (isCompressMode) R.drawable.close_24px else R.drawable.arrow_back_24px),
                    contentDescription = if (isCompressMode) "Cancel" else "Back",
                )
            }

            if (isCompressMode) {
                val compressionStatus = remember(
                    uncompressedOriginalBytes,
                    exifInfo,
                    currentPreview,
                    hasUncompressedOriginal,
                    selectedPreset,
                    appliedPreset,
                    isGeneratingPreview
                ) {
                    val origBytes = uncompressedOriginalBytes ?: exifInfo?.fileSizeBytes ?: currentPreview?.originalSizeBytes ?: 0L
                    val originalSizeStr = if (origBytes > 0L) origBytes.formatBytes() else (exifInfo?.fileSizeFormatted ?: "")
                    val isRevertingToOriginal = hasUncompressedOriginal && selectedPreset == CompressionPreset.ORIGINAL && appliedPreset != CompressionPreset.ORIGINAL
                    val isCurrentState = selectedPreset == null || (appliedPreset != null && selectedPreset == appliedPreset) ||
                        (!hasUncompressedOriginal && selectedPreset == CompressionPreset.ORIGINAL) ||
                        (currentPreview?.isAlreadyOptimal == true && !isRevertingToOriginal)

                    val subtitleText = when {
                        isGeneratingPreview -> "Estimating..."
                        isCurrentState -> exifInfo?.fileSizeFormatted ?: originalSizeStr
                        selectedPreset == CompressionPreset.ORIGINAL -> originalSizeStr
                        currentPreview != null -> {
                            val newStr = currentPreview.compressedSizeBytes.formatBytes()
                            "$originalSizeStr → $newStr"
                        }
                        else -> originalSizeStr
                    }

                    val canSave = selectedPreset != null && !isGeneratingPreview && !isCurrentState && (
                        isRevertingToOriginal || (
                            selectedPreset != CompressionPreset.ORIGINAL &&
                            currentPreview != null &&
                            !currentPreview.isAlreadyOptimal &&
                            (hasUncompressedOriginal || currentPreview.savingsPercent > 0)
                        )
                    )

                    CompressionTopBarStatus(
                        isCurrentState = isCurrentState,
                        subtitleText = subtitleText,
                        canSave = canSave
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = selectedPreset?.label ?: "...",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (compressionStatus.isCurrentState) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.check_circle_24px_fill),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = compressionStatus.subtitleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val canSave = compressionStatus.canSave

                FilledIconButton(
                    onClick = onSaveCompression,
                    enabled = canSave,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (canSave) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        contentColor = if (canSave) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check_24px),
                        contentDescription = "Save",
                    )
                }
            } else {
                val topDateMillis = exifInfo?.dateTakenMillis
                    ?: dateTime
                    ?: parentJournal?.dateTime
                    ?: (if (currentPath != null) java.io.File(currentPath).lastModified() else System.currentTimeMillis())

                Text(
                    text = topDateMillis.toMediumDate(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                )

                var showMenu by remember { mutableStateOf(false) }

                Box {
                    FilledIconButton(
                        onClick = { showMenu = true },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (showMenu) MaterialTheme.colorScheme.primaryContainer else if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (showMenu) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert_24px),
                            contentDescription = "More options",
                        )
                    }

                    val canCompressFurther = availablePresets.any { it != CompressionPreset.ORIGINAL } || hasUncompressedOriginal

                    JuneDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        JuneDropdownMenuItem(
                            text = { Text("Info") },
                            onClick = {
                                showMenu = false
                                onOpenInfo()
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.info_24px), null) }
                        )
                        if (allowCompression) {
                            JuneDropdownMenuItem(
                                text = { Text("Compress") },
                                onClick = {
                                    showMenu = false
                                    onStartCompression()
                                },
                                enabled = !isCompressing && canCompressFurther,
                                leadingIcon = { Icon(painterResource(R.drawable.compress_24px), null) }
                            )
                        }
                        if (onDeleteMedia != null) {
                            JuneDropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    showMenu = false
                                    onDeleteMedia()
                                },
                                leadingIcon = { Icon(painterResource(R.drawable.delete_24px), null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class CompressionTopBarStatus(
    val isCurrentState: Boolean,
    val subtitleText: String,
    val canSave: Boolean
)
