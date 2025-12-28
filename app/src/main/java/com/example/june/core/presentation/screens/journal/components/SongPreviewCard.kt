package com.example.june.core.presentation.screens.journal.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.june.core.domain.data_classes.SongDetails
import com.example.june.core.presentation.components.SongPlayerCard
import com.example.june.core.presentation.utils.rememberManagedExoPlayer
import kotlinx.coroutines.delay
import com.example.june.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SongPreviewCard(
    details: SongDetails?,
    isFetching: Boolean,
    onRemove: () -> Unit
) {
    val exoPlayer = details?.previewUrl?.let {
        rememberManagedExoPlayer(uri = Uri.parse(it), repeatMode = Player.REPEAT_MODE_OFF)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    if (details != null) {
        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        sliderValue = 0f
                        exoPlayer?.seekTo(0)
                    }
                }
            }
            exoPlayer?.addListener(listener)
            onDispose { exoPlayer?.removeListener(listener) }
        }

        LaunchedEffect(isPlaying, isSeeking) {
            while (isPlaying && !isSeeking) {
                exoPlayer?.let { player ->
                    val duration = player.duration.coerceAtLeast(1)
                    val position = player.currentPosition
                    sliderValue = position.toFloat() / duration.toFloat()
                }
                delay(100)
            }
        }
    }

    when {
        isFetching -> {
            SongCardPlaceholder(isLoading = true)
        }

        details != null -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                SongPlayerCard(
                    details = details,
                    isPlaying = isPlaying,
                    sliderValue = sliderValue,
                    onPlayPause = { if (isPlaying) exoPlayer?.pause() else exoPlayer?.play() },
                    onSeek = { newVal ->
                        isSeeking = true
                        sliderValue = newVal
                        exoPlayer?.let { player ->
                            player.seekTo((newVal * player.duration).toLong())
                        }
                    },
                    onSeekFinished = { isSeeking = false },
                )
                Box(
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    RemoveChip(onRemove)
                }
            }
        }

        else -> {
            SongCardPlaceholder(isLoading = false)
        }
    }
}

@Composable
fun RemoveChip(
    onRemove: () -> Unit
) {
    Surface(
        onClick = onRemove,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.close_24px),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Remove",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongCardPlaceholder(
    isLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(64.dp),
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.music_note_2_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No song attached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}