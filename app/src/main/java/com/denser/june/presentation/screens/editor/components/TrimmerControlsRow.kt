package com.denser.june.presentation.screens.editor.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

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

        val isFullTrack = clipDurationMs >= totalDurationMs

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
                    .then(
                        if (!isFullTrack) {
                            Modifier.pointerInput(totalDurationMs, clipDurationMs) {
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
                            }
                        } else Modifier
                    ),
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
