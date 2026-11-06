package com.denser.june.presentation.screens.editor.components.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformScrubber(
    waveform: List<Float>,
    sliderValue: Float,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.5.dp,
    barSpacing: Dp = 3.5.dp,
    isInteractive: Boolean = true
) {
    val playedColor = MaterialTheme.colorScheme.primary
    val unplayedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val playheadColor = MaterialTheme.colorScheme.primary

    val effectiveWaveform = remember(waveform) {
        val raw = if (waveform.isNotEmpty()) waveform else List(80) { (0.15f + (it % 7) * 0.12f) }
        val maxAmp = raw.maxOrNull() ?: 1f
        val normFactor = if (maxAmp > 0.05f) 1f / maxAmp else 1f
        raw.map { (it * normFactor).coerceIn(0.08f, 1f) }
    }

    val totalBars = effectiveWaveform.size
    val currentSliderValue by rememberUpdatedState(sliderValue)

    Canvas(
        modifier = modifier
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(totalBars) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            var accumulated = currentSliderValue
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    onSeekFinished()
                                    break
                                }
                                val dragAmount = change.position.x - change.previousPosition.x
                                if (dragAmount != 0f) {
                                    change.consume()
                                    val stepPx = (barWidth + barSpacing).toPx()
                                    val totalWaveformWidth = (totalBars - 1) * stepPx
                                    if (totalWaveformWidth > 0f) {
                                        val fractionDelta = -dragAmount / totalWaveformWidth
                                        accumulated = (accumulated + fractionDelta).coerceIn(0f, 1f)
                                        onSeek(accumulated)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val verticalPadding = 6.dp.toPx()
        val maxWaveformHeight = (height - (2 * verticalPadding)).coerceIn(20.dp.toPx(), 68.dp.toPx())
        val playheadHalfHeight = maxWaveformHeight / 2f + 3.dp.toPx()

        val barWidthPx = barWidth.toPx()
        val step = (barWidth + barSpacing).toPx()

        val playheadIndex = sliderValue * (totalBars - 1).toFloat()

        for (i in 0 until totalBars) {
            val barX = centerX + (i - playheadIndex) * step
            if (barX < -barWidthPx || barX > width + barWidthPx) continue

            val amp = effectiveWaveform[i].coerceIn(0.08f, 1f)
            val barHeight = (amp * maxWaveformHeight).coerceAtLeast(4.dp.toPx())
            val top = centerY - (barHeight / 2f)
            val color = if (barX <= centerX) playedColor else unplayedColor

            drawLine(
                color = color,
                start = Offset(barX, top),
                end = Offset(barX, top + barHeight),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round
            )
        }

        drawLine(
            color = playheadColor,
            start = Offset(centerX, centerY - playheadHalfHeight),
            end = Offset(centerX, centerY + playheadHalfHeight),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
