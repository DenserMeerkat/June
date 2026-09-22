package com.denser.june.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.presentation.theme.LocalInternetAllowed
import kotlinx.coroutines.delay

data class SongPlayerState(
    val exoPlayer: ExoPlayer?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val sliderValue: Float,
    val isSeeking: Boolean,
    val isRepeatEnabled: Boolean,
    val onPlayPause: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onSeekFinished: () -> Unit,
    val onToggleRepeat: () -> Unit
)

@Composable
fun rememberSongPlayerState(
    previewUrl: String?,
    localPreviewPath: String? = null,
    clipStartMs: Long? = null,
    clipEndMs: Long? = null,
    autoRepeat: Boolean = false
): SongPlayerState {
    val context = LocalContext.current
    val isInternetAllowed = LocalInternetAllowed.current
    val localFile = remember(localPreviewPath) {
        FileUtils.resolveSongMedia(context, localPreviewPath, "library")
    }
    val uri = remember(localFile, previewUrl, isInternetAllowed) {
        if (localFile != null) {
            localFile.toUri()
        } else if (isInternetAllowed) {
            previewUrl?.toUri()
        } else {
            null
        }
    }
    val exoPlayer = uri?.let {
        rememberManagedExoPlayer(uri = it, repeatMode = Player.REPEAT_MODE_OFF)
    }

    val startMs = remember(clipStartMs) { (clipStartMs ?: 0L).coerceAtLeast(0L) }

    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var isRepeatEnabled by remember(autoRepeat) { mutableStateOf(autoRepeat) }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer, startMs, clipEndMs) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        if (exoPlayer.currentPosition < startMs) {
                            exoPlayer.seekTo(startMs)
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        if (!isRepeatEnabled) {
                            isPlaying = false
                            sliderValue = 0f
                            exoPlayer.seekTo(startMs)
                            exoPlayer.pause()
                        } else {
                            exoPlayer.seekTo(startMs)
                            exoPlayer.play()
                        }
                    }
                }
            }
            exoPlayer.addListener(listener)

            isLoading = exoPlayer.playbackState == Player.STATE_BUFFERING
            isPlaying = exoPlayer.isPlaying
            if (exoPlayer.currentPosition < startMs) {
                exoPlayer.seekTo(startMs)
            }

            onDispose { exoPlayer.removeListener(listener) }
        }

        LaunchedEffect(isPlaying, isSeeking, startMs, clipEndMs, isRepeatEnabled) {
            while (isPlaying && !isSeeking) {
                val totalDuration = exoPlayer.duration.coerceAtLeast(1L)
                val effectiveEndMs = clipEndMs?.coerceAtMost(totalDuration)?.takeIf { it > startMs } ?: totalDuration
                val clipSpan = (effectiveEndMs - startMs).coerceAtLeast(1L)
                val currentPos = exoPlayer.currentPosition

                if (clipEndMs != null && currentPos >= effectiveEndMs) {
                    if (isRepeatEnabled) {
                        exoPlayer.seekTo(startMs)
                    } else {
                        isPlaying = false
                        sliderValue = 0f
                        exoPlayer.seekTo(startMs)
                        exoPlayer.pause()
                    }
                } else {
                    val offsetInClip = (currentPos - startMs).coerceIn(0L, clipSpan)
                    sliderValue = offsetInClip.toFloat() / clipSpan.toFloat()
                }
                delay(50)
            }
        }
    }

    val onPlayPause = {
        if (isPlaying) {
            exoPlayer?.pause()
        } else {
            exoPlayer?.let { player ->
                val totalDuration = player.duration.coerceAtLeast(1L)
                val effectiveEndMs = clipEndMs?.coerceAtMost(totalDuration)?.takeIf { it > startMs } ?: totalDuration
                val currentPos = player.currentPosition
                if (currentPos < startMs || (clipEndMs != null && currentPos >= effectiveEndMs)) {
                    player.seekTo(startMs)
                }
                player.play()
            }
        }
        Unit
    }

    val onSeek: (Float) -> Unit = { newVal ->
        isSeeking = true
        sliderValue = newVal
        exoPlayer?.let { player ->
            val totalDuration = player.duration.coerceAtLeast(1L)
            val effectiveEndMs = clipEndMs?.coerceAtMost(totalDuration)?.takeIf { it > startMs } ?: totalDuration
            val clipSpan = (effectiveEndMs - startMs).coerceAtLeast(1L)
            val targetMs = startMs + (newVal * clipSpan).toLong()
            player.seekTo(targetMs)
        }
    }

    val onSeekFinished = {
        isSeeking = false
        Unit
    }

    val onToggleRepeat = {
        isRepeatEnabled = !isRepeatEnabled
        if (clipEndMs == null) {
            exoPlayer?.repeatMode = if (isRepeatEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        } else {
            exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
        }
        Unit
    }

    return remember(exoPlayer, isPlaying, isLoading, sliderValue, isSeeking, isRepeatEnabled) {
        SongPlayerState(
            exoPlayer = exoPlayer,
            isPlaying = isPlaying,
            isLoading = isLoading,
            sliderValue = sliderValue,
            isSeeking = isSeeking,
            isRepeatEnabled = isRepeatEnabled,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            onToggleRepeat = onToggleRepeat
        )
    }
}

fun formatAudioTimestamp(ms: Long): String = ms.toAudioTimestamp()