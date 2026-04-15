package com.denser.june.presentation.utils

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
    val isRepeatEnabled: Boolean,
    val onPlayPause: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onSeekFinished: () -> Unit,
    val onToggleRepeat: () -> Unit
)

@Composable
fun rememberSongPlayerState(
    previewUrl: String?,
    isInternetAllowed: Boolean = true
): SongPlayerState {
    val uri = remember(previewUrl, isInternetAllowed) {
        if (isInternetAllowed) previewUrl?.toUri() else null
    }
    val exoPlayer = uri?.let {
        rememberManagedExoPlayer(uri = it, repeatMode = Player.REPEAT_MODE_OFF)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var isRepeatEnabled by remember { mutableStateOf(false) }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_ENDED) {
                        if (!isRepeatEnabled) {
                            isPlaying = false
                            sliderValue = 0f
                            exoPlayer.seekTo(0)
                            exoPlayer.pause()
                        }
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

    val onToggleRepeat = {
        isRepeatEnabled = !isRepeatEnabled
        exoPlayer?.repeatMode = if (isRepeatEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
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