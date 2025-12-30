package com.example.june.core.presentation.utils

import androidx.compose.runtime.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import androidx.core.net.toUri

data class SongPlayerState(
    val exoPlayer: ExoPlayer?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val sliderValue: Float,
    val isSeeking: Boolean,
    val onPlayPause: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onSeekFinished: () -> Unit
)

@Composable
fun rememberSongPlayerState(
    previewUrl: String?,
): SongPlayerState {
    val uri = remember(previewUrl) { previewUrl?.toUri() }
    val exoPlayer = uri?.let {
        rememberManagedExoPlayer(uri = it, repeatMode = Player.REPEAT_MODE_OFF)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        sliderValue = 0f
                        exoPlayer.seekTo(0)
                        exoPlayer.pause()
                    }
                }
            }
            exoPlayer.addListener(listener)

            isLoading = exoPlayer.playbackState == Player.STATE_BUFFERING
            isPlaying = exoPlayer.isPlaying

            onDispose { exoPlayer.removeListener(listener) }
        }

        LaunchedEffect(isPlaying, isSeeking) {
            while (isPlaying && !isSeeking) {
                val duration = exoPlayer.duration.coerceAtLeast(1)
                val position = exoPlayer.currentPosition
                sliderValue = position.toFloat() / duration.toFloat()
                delay(100)
            }
        }
    }

    val onPlayPause = {
        if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
        Unit
    }

    val onSeek: (Float) -> Unit = { newVal ->
        isSeeking = true
        sliderValue = newVal
        exoPlayer?.let { player ->
            player.seekTo((newVal * player.duration).toLong())
        }
    }

    val onSeekFinished = {
        isSeeking = false
        Unit
    }

    return remember(exoPlayer, isPlaying, isLoading, sliderValue, isSeeking) {
        SongPlayerState(
            exoPlayer = exoPlayer,
            isPlaying = isPlaying,
            isLoading = isLoading,
            sliderValue = sliderValue,
            isSeeking = isSeeking,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished
        )
    }
}