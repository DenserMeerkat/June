@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.june.core.presentation.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.june.R
import com.example.june.core.domain.utils.toHoursMinutesSeconds
import com.example.june.core.presentation.utils.rememberManagedExoPlayer
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun JuneVideoPlayer(
    uri: Uri,
    playWhenReady: Boolean,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var currentTime by remember { mutableLongStateOf(0L) }

    val exoPlayer = rememberManagedExoPlayer(
        uri = uri,
        onIsPlayingChanged = { isPlaying = it }
    )

    LaunchedEffect(playWhenReady) {
        exoPlayer.playWhenReady = playWhenReady
        if (playWhenReady) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentTime = exoPlayer.currentPosition.coerceAtLeast(0L)
            totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            delay(3000)
            onVisibilityChange(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onVisibilityChange(!isVisible)
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    useController = false
                    controllerAutoShow = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isVisible || !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            JuneIconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    onVisibilityChange(true)
                                },
                                buttonSize = 44.dp,
                                iconSize = 32.dp,
                                icon = if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White
                                )
                            )

                            Text(
                                text = "${currentTime.toHoursMinutesSeconds()} / ${totalDuration.toHoursMinutesSeconds()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )

                            JuneIconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    onVisibilityChange(true)
                                },
                                buttonSize = 44.dp,
                                iconSize = 32.dp,
                                icon = if (isMuted) R.drawable.volume_off_24px else R.drawable.volume_up_24px,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White
                                )
                            )
                        }
                        val sliderColors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.6f)
                        )

                        Slider(
                            value = currentTime.toFloat(),
                            onValueChange = {
                                currentTime = it.toLong()
                                exoPlayer.seekTo(it.toLong())
                                onVisibilityChange(true)
                            },
                            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                            colors = sliderColors,
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    colors = sliderColors,
                                    modifier = Modifier.height(8.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp, 16.dp)
                        )
                    }
                }
            }
        }
    }
}