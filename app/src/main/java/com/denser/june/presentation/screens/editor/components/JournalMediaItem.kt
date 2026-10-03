package com.denser.june.presentation.screens.editor.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.denser.june.core.R
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.core.utils.toHoursMinutesSeconds
import com.denser.june.presentation.utils.rememberManagedExoPlayer
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalMediaItem(
    path: String,
    modifier: Modifier,
    operations: MediaOperations,
    isLargeItem: Boolean,
    enablePlayback: Boolean = true,
    enableContextMenu: Boolean = true,
    isProcessing: Boolean = false,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

    val isVideo = remember(path) {
        if (path.startsWith("content://")) {
            context.contentResolver.getType(Uri.parse(path))?.startsWith("video") == true
        } else {
            path.endsWith("mp4", ignoreCase = true)
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var currentTimestamp by remember { mutableLongStateOf(0L) }

    val showVideoPlayer = isVideo && enablePlayback && !isProcessing

    val shouldShowMoveToFront = path != operations.frontMediaPath
    val shouldCaptureTouch = !isProcessing && (enablePlayback || operations.onMediaClick != null)

    val resolvedFile = remember(path) {
        if (path.startsWith("content://") || path.startsWith("file://")) null
        else MediaStorageManager.resolveMediaFile(context, path)
    }
    val imageModel = remember(path, resolvedFile?.lastModified(), resolvedFile?.length()) {
        if (resolvedFile != null) {
            ImageRequest.Builder(context)
                .data(resolvedFile)
                .memoryCacheKey("${resolvedFile.absolutePath}_${resolvedFile.lastModified()}_${resolvedFile.length()}")
                .diskCacheKey("${resolvedFile.absolutePath}_${resolvedFile.lastModified()}_${resolvedFile.length()}")
                .crossfade(true)
                .build()
        } else {
            Uri.parse(path)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .indication(interactionSource, LocalIndication.current)
            .then(
                if (shouldCaptureTouch) {
                    Modifier.pointerInput(operations.onMediaClick, enableContextMenu) {
                        detectTapGestures(
                            onTap = { operations.onMediaClick?.invoke(path) },
                            onLongPress = { offset ->
                                if (enableContextMenu) {
                                    showMenu = true
                                    pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                                }
                            },
                            onPress = { offset ->
                                val press = PressInteraction.Press(offset)
                                interactionSource.emit(press)
                                tryAwaitRelease()
                                interactionSource.emit(PressInteraction.Release(press))
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        if (showVideoPlayer) {
            VideoPlayer(
                uri = if (path.startsWith("content://")) Uri.parse(path) else Uri.fromFile(File(path)),
                isMuted = isMuted,
                isPlaying = isPlaying,
                onPlayingChanged = { isPlaying = it },
                onTimestampChanged = { currentTimestamp = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            if (isVideo && !isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play_arrow_24px),
                        contentDescription = "Video",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .padding(4.dp)
                    )
                }
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(if (isLargeItem) 56.dp else 44.dp)
                )
            }
        }

        if (showVideoPlayer) {
            PlayPauseOverlay(
                isPlaying = isPlaying,
                onClick = { isPlaying = !isPlaying }
            )
        }

        if (showVideoPlayer && isLargeItem) {
            MuteTimeChip(
                isMuted = isMuted,
                timestamp = currentTimestamp.toHoursMinutesSeconds(),
                onToggleMute = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 8.dp)
            )
        }

        if (showMenu && enableContextMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(align = Alignment.TopStart)
                    .offset(x = pressOffset.x, y = pressOffset.y)
                    .size(1.dp)
            ) {
                JuneDropdownMenu(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 200.dp),
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    JuneDropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            showMenu = false
                            isPlaying = false
                            operations.onRemoveMedia(path)
                        },
                        leadingIcon = { Icon(painterResource(R.drawable.delete_24px), null) }
                    )
                    if (shouldShowMoveToFront) {
                        JuneDropdownMenuItem(
                            text = { Text(stringResource(R.string.move_to_front)) },
                            onClick = {
                                operations.onMoveToFront(path)
                                showMenu = false
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.turn_left_24px), null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayPauseOverlay(isPlaying: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(0.4f), CircleShape)
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun MuteTimeChip(
    isMuted: Boolean,
    timestamp: String,
    onToggleMute: () -> Unit,
    modifier: Modifier
) {
    Surface(
        onClick = onToggleMute,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(if (isMuted) R.drawable.volume_off_24px else R.drawable.volume_up_24px),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                timestamp,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    isMuted: Boolean,
    isPlaying: Boolean,
    onPlayingChanged: (Boolean) -> Unit,
    onTimestampChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val exoPlayer = rememberManagedExoPlayer(
        uri = uri,
        onIsPlayingChanged = onPlayingChanged
    )

    LaunchedEffect(isPlaying) { if (isPlaying) exoPlayer.play() else exoPlayer.pause() }
    LaunchedEffect(isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) onTimestampChanged(exoPlayer.currentPosition)
            delay(500)
        }
    }

    AndroidView(
        factory = { ctx ->
            val playerView = LayoutInflater.from(ctx)
                .inflate(R.layout.custom_player_view, null, false) as PlayerView
            playerView.player = exoPlayer
            playerView
        },
        modifier = modifier
    )
}