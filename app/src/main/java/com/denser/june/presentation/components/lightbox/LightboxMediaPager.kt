package com.denser.june.presentation.components.lightbox

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.denser.june.core.media.compress.CompressUtils
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.exif.ExifUtils
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.presentation.components.JuneVideoPlayer

@Composable
fun LightboxMediaPager(
    pagerState: PagerState,
    mediaPaths: List<String>,
    context: Context,
    imageLoader: ImageLoader,
    compressionState: LightboxCompressionState,
    showUI: Boolean,
    isInfoVisible: Boolean,
    isVideoZoomed: Boolean,
    isDarkTheme: Boolean,
    refreshTrigger: Int,
    onToggleUI: () -> Unit,
    onShowInfo: () -> Unit,
    onHideInfo: () -> Unit,
    onVideoZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        pageSpacing = 16.dp,
        userScrollEnabled = !compressionState.isCompressMode && !isInfoVisible && !isVideoZoomed
    ) { page ->
        if (page < mediaPaths.size) {
            val path = mediaPaths[page]
            val itemIsVideo = remember(path) { ExifUtils.isVideoPath(path) }
            val activePath = if (compressionState.isCompressMode && page == pagerState.currentPage) {
                val preset = compressionState.selectedPreset
                if (preset != null && preset != CompressionPreset.ORIGINAL) {
                    compressionState.sessionPreviewCache[preset]?.previewPath ?: path
                } else {
                    CompressUtils.getUncompressedOriginalFile(context, path)?.absolutePath ?: path
                }
            } else {
                path
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (itemIsVideo) {
                    key(activePath, refreshTrigger) {
                        JuneVideoPlayer(
                            uri = Uri.fromFile(MediaStorageManager.resolveMediaFile(context, activePath)),
                            playWhenReady = false,
                            isVisible = showUI && !isInfoVisible && !compressionState.isCompressMode,
                            onVisibilityChange = { onToggleUI() },
                            onSwipeUp = {
                                if (!isInfoVisible && !compressionState.isCompressMode) onShowInfo()
                            },
                            isInfoVisible = isInfoVisible,
                            isCompressMode = compressionState.isCompressMode,
                            onZoomChanged = onVideoZoomChanged,
                            isDarkTheme = isDarkTheme
                        )
                    }
                } else {
                    LightboxZoomableImage(
                        path = activePath,
                        imageLoader = imageLoader,
                        refreshKey = refreshTrigger,
                        onTap = {
                            if (isInfoVisible) {
                                onHideInfo()
                            } else if (!compressionState.isCompressMode) {
                                onToggleUI()
                            }
                        },
                        onSwipeUp = {
                            if (!isInfoVisible && !compressionState.isCompressMode) {
                                onShowInfo()
                            }
                        }
                    )
                }

                val previewResult = compressionState.currentPreview
                if (compressionState.isCompressMode && page == pagerState.currentPage && itemIsVideo && compressionState.selectedPreset != null && compressionState.selectedPreset != CompressionPreset.ORIGINAL && !compressionState.isGeneratingPreview && previewResult?.isClipped == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "15s Preview",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (compressionState.isCompressMode && page == pagerState.currentPage && compressionState.isGeneratingPreview) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
