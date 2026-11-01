@file:kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.denser.june.presentation.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.denser.june.core.R
import com.denser.june.core.utils.toHoursMinutesSeconds
import com.denser.june.presentation.utils.rememberManagedExoPlayer
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(UnstableApi::class)
@Composable
fun JuneVideoPlayer(
    uri: Uri,
    playWhenReady: Boolean,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    onSwipeUp: (() -> Unit)? = null,
    isInfoVisible: Boolean = false,
    isCompressMode: Boolean = false,
    onZoomChanged: ((Boolean) -> Unit)? = null,
    isDarkTheme: Boolean = true
) {
    var isPlaying by remember { mutableStateOf(false) }
    var wasPlayingBeforeInfo by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var currentTime by remember { mutableLongStateOf(0L) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val exoPlayer = rememberManagedExoPlayer(
        uri = uri,
        onIsPlayingChanged = { isPlaying = it }
    )

    LaunchedEffect(isInfoVisible) {
        if (isInfoVisible) {
            zoomScale = 1f
            panOffsetX = 0f
            panOffsetY = 0f
            onZoomChanged?.invoke(false)
            if (isPlaying) {
                wasPlayingBeforeInfo = true
                exoPlayer.pause()
            }
        } else {
            if (wasPlayingBeforeInfo) {
                wasPlayingBeforeInfo = false
                exoPlayer.play()
            }
        }
    }

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

    val currentOnVisibilityChange by rememberUpdatedState(onVisibilityChange)
    val currentIsVisible by rememberUpdatedState(isVisible)
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnZoomChanged by rememberUpdatedState(onZoomChanged)

    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            delay(3000)
            onVisibilityChange(false)
        }
    }

    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color.Black else Color.White),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val playerView = LayoutInflater.from(ctx)
                    .inflate(R.layout.lightbox_player_view, null, false) as PlayerView

                playerView.apply {
                    player = exoPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    isClickable = false
                    isFocusable = false
                    setBackgroundColor(if (isDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    setShutterBackgroundColor(if (isDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            },
            update = { playerView ->
                playerView.setBackgroundColor(if (isDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                playerView.setShutterBackgroundColor(if (isDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isInfoVisible, touchSlop) {
                    if (isInfoVisible) return@pointerInput
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val downX = firstDown.position.x
                        val downY = firstDown.position.y
                        var currentX = downX
                        var currentY = downY
                        var secondPointerId: PointerId? = null
                        var initialSpan = 0f
                        var multiTouch = false
                        var isHorizontalDrag = false
                        var isVerticalSwipe = false

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val primary = event.changes.firstOrNull { it.id == firstDown.id }

                            if (secondPointerId == null && event.changes.count { it.pressed } >= 2) {
                                val second = event.changes.firstOrNull { it.id != firstDown.id && it.pressed }
                                if (second != null) {
                                    secondPointerId = second.id
                                    val p1 = event.changes.first { it.id == firstDown.id }.position
                                    val p2 = second.position
                                    initialSpan = sqrt((p2.x - p1.x).let { it * it } + (p2.y - p1.y).let { it * it })
                                    multiTouch = true
                                }
                            }

                            if (multiTouch && secondPointerId != null) {
                                val p1c = event.changes.firstOrNull { it.id == firstDown.id }
                                val p2c = event.changes.firstOrNull { it.id == secondPointerId }
                                if (p1c != null && p2c != null && p1c.pressed && p2c.pressed) {
                                    val p1 = p1c.position
                                    val p2 = p2c.position
                                    val span = sqrt((p2.x - p1.x).let { it * it } + (p2.y - p1.y).let { it * it })
                                    if (initialSpan > 0f) {
                                        val factor = span / initialSpan
                                        val newScale = (zoomScale * factor).coerceIn(1f, 4f)
                                        zoomScale = newScale
                                        if (newScale <= 1.05f) {
                                            panOffsetX = 0f
                                            panOffsetY = 0f
                                            currentOnZoomChanged?.invoke(false)
                                        } else {
                                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                                            val maxPanY = (size.height * (newScale - 1f)) / 2f
                                            panOffsetX = panOffsetX.coerceIn(-maxPanX, maxPanX)
                                            panOffsetY = panOffsetY.coerceIn(-maxPanY, maxPanY)
                                            currentOnZoomChanged?.invoke(true)
                                        }
                                        initialSpan = span
                                    }
                                    p1c.consume()
                                    p2c.consume()
                                } else {
                                    break
                                }
                                continue
                            }

                            if (primary == null || !primary.pressed) {
                                if (!multiTouch && !isHorizontalDrag && !isVerticalSwipe) {
                                    val elapsed = System.currentTimeMillis() - downTime
                                    val dist = sqrt((currentX - downX) * (currentX - downX) + (currentY - downY) * (currentY - downY))
                                    if (dist < 40f && elapsed < 500) {
                                        if (isCompressMode) {
                                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        } else {
                                            currentOnVisibilityChange(!currentIsVisible)
                                        }
                                    }
                                }
                                break
                            }

                            val dx = primary.position.x - downX
                            val dy = primary.position.y - downY
                            currentX = primary.position.x
                            currentY = primary.position.y

                            val verticalSwipeThreshold = touchSlop * 0.4f
                            val horizontalBreakoutThreshold = touchSlop * 2.5f

                            if (!multiTouch && zoomScale <= 1.05f) {
                                if (!isHorizontalDrag && dy < -verticalSwipeThreshold && abs(dy) > abs(dx) * 0.7f && currentOnSwipeUp != null) {
                                    isVerticalSwipe = true
                                    currentOnSwipeUp?.invoke()
                                    primary.consume()
                                    break
                                }
                                if (!isVerticalSwipe && abs(dx) > horizontalBreakoutThreshold && abs(dx) > abs(dy) * 1.5f && dy >= -verticalSwipeThreshold) {
                                    isHorizontalDrag = true
                                    break
                                }
                            } else if (!multiTouch && zoomScale > 1.05f) {
                                val delta = primary.positionChange()
                                val maxPanX = (size.width * (zoomScale - 1f)) / 2f
                                val maxPanY = (size.height * (zoomScale - 1f)) / 2f
                                panOffsetX = (panOffsetX + delta.x).coerceIn(-maxPanX, maxPanX)
                                panOffsetY = (panOffsetY + delta.y).coerceIn(-maxPanY, maxPanY)
                                primary.consume()
                            }
                        }
                    }
                }
        )

        if (isCompressMode) {
            AnimatedVisibility(
                visible = !isPlaying || isVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                FilledIconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = (if (isDarkTheme) Color.Black else Color.White).copy(alpha = 0.65f),
                        contentColor = if (isDarkTheme) Color.White else Color.Black
                    )
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isCompressMode && !isInfoVisible && (isVisible || !isPlaying),
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
                                colors = if (isDarkTheme) {
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                } else {
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.9f))
                                }
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    onVisibilityChange(true)
                                },
                                modifier = Modifier.size(48.dp),
                                shape = IconButtonDefaults.smallRoundShape,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    painter = painterResource(if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "${currentTime.toHoursMinutesSeconds()} / ${totalDuration.toHoursMinutesSeconds()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    onVisibilityChange(true)
                                },
                                modifier = Modifier.size(48.dp),
                                shape = IconButtonDefaults.smallRoundShape,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    painter = painterResource(if (isMuted) R.drawable.volume_off_24px else R.drawable.volume_up_24px),
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        val sliderColors = SliderDefaults.colors(
                            thumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHighest
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