package com.example.june.core.presentation.screens.journal.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.example.june.R
import com.example.june.core.domain.utils.formatDuration
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun JournalMediaItem(
    path: String,
    modifier: Modifier,
    operations : MediaOperations,
    isLargeItem: Boolean,
    enablePlayback: Boolean = true,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }

    val isVideo = remember(path) { path.endsWith("mp4", ignoreCase = true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentTimestamp by remember { mutableLongStateOf(0L) }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }
    val shouldShowMoveToFront = path != operations.frontMediaPath
    val shouldCaptureTouches = operations.isEditMode || operations.onMediaClick != null

    Box(
        modifier = modifier
            .onSizeChanged { itemHeight = with(density) { it.height.toDp() } }
            .clip(RoundedCornerShape(4.dp))
            .indication(interactionSource, LocalIndication.current)
            .then(
                if (shouldCaptureTouches) {
                    Modifier.pointerInput(operations.isEditMode, operations.onMediaClick) {
                        detectTapGestures(
                            onTap = { operations.onMediaClick?.invoke(path) },
                            onLongPress = { offset ->
                                if (operations.isEditMode) {
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
        if (enablePlayback && isVideo) {
            VideoPlayer(
                uri = Uri.fromFile(File(path)),
                isMuted = isMuted,
                isPlaying = isPlaying,
                onTimestampChanged = { currentTimestamp = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = File(path),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            if (isVideo) {
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

        if (isVideo && enablePlayback) {
            PlayPauseOverlay(
                isPlaying = isPlaying,
                onClick = { isPlaying = !isPlaying }
            )
        }

        if (enablePlayback && isLargeItem && isVideo) {
            MuteTimeChip(
                isMuted = isMuted,
                timestamp = currentTimestamp.formatDuration(),
                onToggleMute = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }

        if (operations.isEditMode) {
            DropdownMenu(
                modifier = Modifier.defaultMinSize(minWidth = 200.dp),
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 3.dp,
                offset = pressOffset.copy(y = pressOffset.y - itemHeight)
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        isPlaying = false
                        operations.onRemove(path)
                        showMenu = false
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.delete_24px), null) }
                )
                if (shouldShowMoveToFront) {
                    DropdownMenuItem(
                        text = { Text("Move to Front") },
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
private fun MuteTimeChip(isMuted: Boolean, timestamp: String, onToggleMute: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onToggleMute,
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(if (isMuted) R.drawable.volume_off_24px else R.drawable.volume_up_24px),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(timestamp, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(uri: Uri, isMuted: Boolean, isPlaying: Boolean, onTimestampChanged: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    LaunchedEffect(isPlaying) { exoPlayer.playWhenReady = isPlaying }
    LaunchedEffect(isMuted) { exoPlayer.volume = if (isMuted) 0f else 1f }
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) onTimestampChanged(exoPlayer.currentPosition)
            delay(500)
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}