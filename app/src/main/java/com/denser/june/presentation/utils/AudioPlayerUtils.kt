package com.denser.june.presentation.utils

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.denser.june.core.utils.toAudioTimestamp
import kotlinx.coroutines.delay
import java.io.File

data class AudioPlayerState(
    val exoPlayer: ExoPlayer?,
    val isPlaying: Boolean,
    val isLoading: Boolean,
    val sliderValue: Float,
    val currentPositionMs: Long,
    val totalDurationMs: Long,
    val remainingMs: Long,
    val playbackSpeed: Float,
    val onPlayPause: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onSeekFinished: () -> Unit,
    val onRewind5s: () -> Unit,
    val onForward10s: () -> Unit,
    val onToggleSpeed: () -> Unit
)

@Composable
fun rememberAudioPlayerState(
    file: File?,
    initialDurationMs: Long = 0L
): AudioPlayerState {
    val context = LocalContext.current
    val uri = remember(file?.absolutePath) {
        file?.takeIf { it.exists() && it.length() > 0L }?.let { Uri.fromFile(it) }
    }

    val exoPlayer = remember(uri) {
        if (uri != null) {
            val renderersFactory = DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setEnableDecoderFallback(true)
            ExoPlayer.Builder(context, renderersFactory).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                this.repeatMode = Player.REPEAT_MODE_OFF
            }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isPlaying by remember(exoPlayer) { mutableStateOf(false) }
    var isLoading by remember(exoPlayer) { mutableStateOf(false) }
    var sliderValue by remember(exoPlayer) { mutableFloatStateOf(0f) }
    var isSeeking by remember(exoPlayer) { mutableStateOf(false) }
    var currentPositionMs by remember(exoPlayer) { mutableLongStateOf(0L) }
    var totalDurationMs by remember(exoPlayer, initialDurationMs) { mutableLongStateOf(initialDurationMs) }
    var playbackSpeed by remember(exoPlayer) { mutableFloatStateOf(1.0f) }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        val dur = exoPlayer.duration
                        if (dur > 0L) totalDurationMs = dur
                    } else if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        sliderValue = 0f
                        currentPositionMs = 0L
                        exoPlayer.seekTo(0L)
                        exoPlayer.pause()
                    }
                }
            }
            exoPlayer.addListener(listener)
            isLoading = exoPlayer.playbackState == Player.STATE_BUFFERING
            isPlaying = exoPlayer.isPlaying
            if (exoPlayer.duration > 0L) totalDurationMs = exoPlayer.duration

            onDispose { exoPlayer.removeListener(listener) }
        }

        LaunchedEffect(isPlaying, isSeeking, totalDurationMs) {
            while (isPlaying && !isSeeking) {
                val dur = if (totalDurationMs > 0L) totalDurationMs else exoPlayer.duration.coerceAtLeast(1L)
                val pos = exoPlayer.currentPosition.coerceIn(0L, dur)
                currentPositionMs = pos
                sliderValue = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                delay(40)
            }
        }
    }

    val onPlayPause: () -> Unit = {
        if (isPlaying) {
            exoPlayer?.pause()
        } else {
            exoPlayer?.let { player ->
                if (player.playerError != null || player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                if (player.playbackState == Player.STATE_ENDED || (totalDurationMs > 0L && player.currentPosition >= totalDurationMs)) {
                    player.seekTo(0L)
                }
                player.play()
            }
        }
    }

    val onSeek: (Float) -> Unit = { fraction ->
        isSeeking = true
        sliderValue = fraction
        val target = (fraction * totalDurationMs).toLong()
        currentPositionMs = target
    }

    val onSeekFinished: () -> Unit = {
        exoPlayer?.seekTo(currentPositionMs)
        isSeeking = false
    }

    val onRewind5s: () -> Unit = {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - 5_000L).coerceAtLeast(0L)
            player.seekTo(target)
            currentPositionMs = target
            if (totalDurationMs > 0L) {
                sliderValue = (target.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val onForward10s: () -> Unit = {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + 10_000L).coerceAtMost(totalDurationMs)
            player.seekTo(target)
            currentPositionMs = target
            if (totalDurationMs > 0L) {
                sliderValue = (target.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val onToggleSpeed: () -> Unit = {
        val nextSpeed = when (playbackSpeed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        playbackSpeed = nextSpeed
        exoPlayer?.playbackParameters = PlaybackParameters(nextSpeed)
    }

    val remainingMs = remember(totalDurationMs, currentPositionMs) {
        (totalDurationMs - currentPositionMs).coerceAtLeast(0L)
    }

    return remember(
        exoPlayer,
        isPlaying,
        isLoading,
        sliderValue,
        currentPositionMs,
        totalDurationMs,
        remainingMs,
        playbackSpeed
    ) {
        AudioPlayerState(
            exoPlayer = exoPlayer,
            isPlaying = isPlaying,
            isLoading = isLoading,
            sliderValue = sliderValue,
            currentPositionMs = currentPositionMs,
            totalDurationMs = totalDurationMs,
            remainingMs = remainingMs,
            playbackSpeed = playbackSpeed,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            onRewind5s = onRewind5s,
            onForward10s = onForward10s,
            onToggleSpeed = onToggleSpeed
        )
    }
}

fun formatRemainingTimestamp(ms: Long): String {
    val formatted = ms.toAudioTimestamp()
    return "-$formatted"
}

