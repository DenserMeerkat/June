package com.denser.june.presentation.screens.editor.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

@Composable
fun AlternatingWaveformScrubber(
    modifier: Modifier = Modifier,
    totalDurationMs: Long,
    clipDurationMs: Long,
    startMs: Long,
    playbackFraction: Float = 0f,
    isPlaying: Boolean = false,
    onScrollStartMs: (Long) -> Unit,
    onDragFinished: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val inactiveBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
    val windowFillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
    val playedOverlayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val windowBorderColor = MaterialTheme.colorScheme.primary
    val handleColor = MaterialTheme.colorScheme.primary
    val handleInnerGripColor = MaterialTheme.colorScheme.onPrimary

    val currentStartMs by rememberUpdatedState(startMs)
    val currentTotalDurationMs by rememberUpdatedState(totalDurationMs)
    val currentClipDurationMs by rememberUpdatedState(clipDurationMs)
    val currentOnScrollStartMs by rememberUpdatedState(onScrollStartMs)
    val currentOnDragFinished by rememberUpdatedState(onDragFinished)
    val currentPlaybackFraction by rememberUpdatedState(playbackFraction)
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    val haptic = LocalHapticFeedback.current
    var isScrubbing by remember { mutableStateOf(false) }
    var lastScrubHapticMs by remember { mutableLongStateOf(0L) }
    var dragStartMs by remember { mutableLongStateOf(0L) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        val windowWidthPx = containerWidthPx * 0.68f
        val windowLeftPx = (containerWidthPx - windowWidthPx) / 2f
        val windowRightPx = windowLeftPx + windowWidthPx

        val safeClipDuration = currentClipDurationMs.coerceAtLeast(1_000L)
        val pxPerMs = windowWidthPx / safeClipDuration.toFloat()
        val totalWaveformWidthPx = currentTotalDurationMs * pxPerMs
        val waveformStartX = windowLeftPx - (currentStartMs * pxPerMs)
        val songEndXPx = waveformStartX + totalWaveformWidthPx

        val minGapDp = 5.dp
        val maxGapDp = 8.dp
        val normalizedDuration = (currentClipDurationMs.toFloat() / 60_000f).coerceIn(0f, 1f)
        val dynamicGapDp = (maxGapDp.value - (normalizedDuration * (maxGapDp.value - minGapDp.value))).dp.coerceIn(minGapDp, maxGapDp)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isScrubbing = true
                            dragStartMs = currentStartMs
                            dragAccumulator = 0f
                            lastScrubHapticMs = currentStartMs
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = {
                            isScrubbing = false
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnDragFinished()
                        },
                        onDragCancel = {
                            isScrubbing = false
                            currentOnDragFinished()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (pxPerMs > 0f) {
                                val dragSensitivity = 0.28f
                                dragAccumulator -= (dragAmount * dragSensitivity) / pxPerMs
                                val maxStart = (currentTotalDurationMs - currentClipDurationMs).coerceAtLeast(0L)
                                val newStart = (dragStartMs + dragAccumulator.toLong()).coerceIn(0L, maxStart)
                                if (kotlin.math.abs(newStart - lastScrubHapticMs) >= 500L || newStart == 0L || newStart == maxStart) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastScrubHapticMs = newStart
                                }
                                currentOnScrollStartMs(newStart)
                            }
                        }
                    )
                }
        ) {
            val barWidthPx = 3.dp.toPx()
            val barGapPx = dynamicGapDp.toPx()
            val stepPx = barWidthPx + barGapPx
            val totalBars = (totalWaveformWidthPx / stepPx).toInt().coerceAtLeast(1) + 1
            val centerY = containerHeightPx / 2f

            val playheadXPx = windowLeftPx + (currentPlaybackFraction.coerceIn(0f, 1f) * windowWidthPx)

            val windowCornerRadius = 14.dp.toPx()
            val windowStrokeWidth = 2.5.dp.toPx()
            val windowTop = 8.dp.toPx()
            val windowHeight = containerHeightPx - (windowTop * 2f)

            val windowRoundRect = RoundRect(
                left = windowLeftPx,
                top = windowTop,
                right = windowRightPx,
                bottom = windowTop + windowHeight,
                cornerRadius = CornerRadius(windowCornerRadius)
            )
            val windowPath = Path().apply {
                addRoundRect(windowRoundRect)
            }

            val playedWidthPx = (playheadXPx - windowLeftPx).coerceIn(0f, windowWidthPx)

            clipPath(windowPath) {
                drawRect(
                    color = windowFillColor,
                    topLeft = Offset(windowLeftPx, windowTop),
                    size = Size(windowWidthPx, windowHeight)
                )
                if ((currentIsPlaying || currentPlaybackFraction > 0f) && playedWidthPx > 0f) {
                    drawRect(
                        color = playedOverlayColor,
                        topLeft = Offset(windowLeftPx, windowTop),
                        size = Size(playedWidthPx, windowHeight)
                    )
                }
            }

            for (i in 0 until totalBars) {
                val barX = waveformStartX + (i * stepPx)
                if (barX + barWidthPx > songEndXPx + 0.5f) break
                if (barX < waveformStartX - 0.5f) continue
                if (barX < -stepPx || barX > containerWidthPx + stepPx) continue

                val isTall = i % 2 == 0
                val rawAmp = if (isTall) 0.88f else 0.48f
                val barHeight = rawAmp * (containerHeightPx * 0.32f)

                val isInsideWindow = barX in (windowLeftPx - 1f)..(windowRightPx + 1f)
                val activeTone = if (isTall) primaryColor else tertiaryColor

                val barColor = if (!isInsideWindow) {
                    inactiveBarColor
                } else if (currentIsPlaying || currentPlaybackFraction > 0f) {
                    if (barX <= playheadXPx) activeTone else activeTone.copy(alpha = 0.38f)
                } else {
                    activeTone
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(barX, centerY - (barHeight / 2f)),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(barWidthPx / 2f)
                )
            }

            if ((currentIsPlaying || currentPlaybackFraction > 0f) && playheadXPx in windowLeftPx..windowRightPx) {
                val playheadWidth = 2.5.dp.toPx()
                val playheadHeight = 34.dp.toPx()
                val playheadTop = centerY - (playheadHeight / 2f)
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(playheadXPx - (playheadWidth / 2f), playheadTop),
                    size = Size(playheadWidth, playheadHeight),
                    cornerRadius = CornerRadius(playheadWidth / 2f)
                )
            }

            drawRoundRect(
                color = windowBorderColor,
                topLeft = Offset(windowLeftPx, windowTop),
                size = Size(windowWidthPx, windowHeight),
                cornerRadius = CornerRadius(windowCornerRadius),
                style = Stroke(width = windowStrokeWidth)
            )

            val handleWidth = 4.dp.toPx()
            val handleHeight = 26.dp.toPx()
            val handleTop = centerY - (handleHeight / 2f)
            val gripWidth = 1.5.dp.toPx()
            val gripHeight = 12.dp.toPx()
            val gripTop = centerY - (gripHeight / 2f)

            drawRoundRect(
                color = handleColor,
                topLeft = Offset(windowLeftPx - (handleWidth / 2f), handleTop),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(handleWidth / 2f)
            )
            drawRoundRect(
                color = handleInnerGripColor,
                topLeft = Offset(windowLeftPx - (gripWidth / 2f), gripTop),
                size = Size(gripWidth, gripHeight),
                cornerRadius = CornerRadius(gripWidth / 2f)
            )

            drawRoundRect(
                color = handleColor,
                topLeft = Offset(windowRightPx - (handleWidth / 2f), handleTop),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(handleWidth / 2f)
            )
            drawRoundRect(
                color = handleInnerGripColor,
                topLeft = Offset(windowRightPx - (gripWidth / 2f), gripTop),
                size = Size(gripWidth, gripHeight),
                cornerRadius = CornerRadius(gripWidth / 2f)
            )
        }
    }
}

@Composable
fun ControlsRow(
    startMs: Long,
    clipDurationMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onDurationSelected: (Long) -> Unit,
    onSeekStartMs: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDurationBottomSheet by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var isDraggingTrack by remember { mutableStateOf(false) }
    var lastHapticSeekMs by remember { mutableLongStateOf(0L) }

    val animatedPillHeight by animateDpAsState(
        targetValue = if (isDraggingTrack) 20.dp else 14.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "trackPillHeight"
    )
    val animatedPillElevation by animateDpAsState(
        targetValue = if (isDraggingTrack) 2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "trackPillElevation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val effectiveMs = if (clipDurationMs >= totalDurationMs) totalDurationMs else clipDurationMs
        val currentSeconds = kotlin.math.round(effectiveMs / 1000.0).toLong()
        val buttonText = "${currentSeconds}s"

        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showDurationBottomSheet = true
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (showDurationBottomSheet) {
            DurationPickerBottomSheet(
                totalDurationMs = totalDurationMs,
                currentDurationMs = clipDurationMs,
                onDurationSelected = onDurationSelected,
                onDismiss = { showDurationBottomSheet = false }
            )
        }

        val safeTotal = totalDurationMs.coerceAtLeast(1L).toFloat()
        val windowStartFraction = (startMs.toFloat() / safeTotal).coerceIn(0f, 1f)
        val windowSpanFraction = (clipDurationMs.toFloat() / safeTotal).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .pointerInput(totalDurationMs, clipDurationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDraggingTrack = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val widthPx = size.width.toFloat()
                                if (widthPx > 0f) {
                                    val maxStartMs = (totalDurationMs - clipDurationMs).coerceAtLeast(0L)
                                    val centerFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                                    val targetCenterMs = (centerFraction * totalDurationMs).toLong()
                                    val newStart = (targetCenterMs - clipDurationMs / 2).coerceIn(0L, maxStartMs)
                                    lastHapticSeekMs = newStart
                                    onSeekStartMs(newStart)
                                }
                            },
                            onDragEnd = {
                                isDraggingTrack = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragCancel = {
                                isDraggingTrack = false
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val widthPx = size.width.toFloat()
                                if (widthPx > 0f) {
                                    val maxStartMs = (totalDurationMs - clipDurationMs).coerceAtLeast(0L)
                                    val centerFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                                    val targetCenterMs = (centerFraction * totalDurationMs).toLong()
                                    val newStart = (targetCenterMs - clipDurationMs / 2).coerceIn(0L, maxStartMs)
                                    if (kotlin.math.abs(newStart - lastHapticSeekMs) >= 1_000L || newStart == 0L || newStart == maxStartMs) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastHapticSeekMs = newStart
                                    }
                                    onSeekStartMs(newStart)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )

                val trackWidth = maxWidth
                val pillOffset = trackWidth * windowStartFraction
                val pillWidth = (trackWidth * windowSpanFraction)
                    .coerceAtMost(trackWidth - pillOffset)
                    .coerceAtLeast(14.dp)

                Surface(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(pillWidth)
                        .height(animatedPillHeight),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = animatedPillElevation,
                    tonalElevation = animatedPillElevation
                ) {}
            }
        }

        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPlayPause()
            },
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
