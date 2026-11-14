package com.denser.june.presentation.screens.editor.components.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import java.util.Locale

@Composable
fun AudioStudioCanvas(
    isRecording: Boolean,
    elapsedMillis: Long,
    amplitudes: List<Float>,
    isPaused: Boolean,
    onStartRecording: () -> Unit,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    hasPermission: Boolean = true,
    onOpenSettings: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val statusIcon = when {
                            !hasPermission -> R.drawable.mic_off_24px
                            !isRecording -> R.drawable.mic_24px_fill
                            isPaused -> R.drawable.pause_24px
                            else -> R.drawable.mic_24px_fill
                        }
                        val statusTint = when {
                            !hasPermission || !isRecording || isPaused -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> Color(0xFFE53935)
                        }
                        val statusText = when {
                            !hasPermission -> "Permission required"
                            !isRecording -> "Ready to record"
                            isPaused -> "Paused"
                            else -> "Recording…"
                        }
                        Icon(
                            painter = painterResource(statusIcon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = statusTint
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .heightIn(min = 72.dp),
                shape = RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    when {
                        !hasPermission -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.mic_off_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Microphone access required",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Allow June to access your microphone to record audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = onOpenSettings,
                                    shape = CircleShape
                                ) {
                                    Text("Open Settings")
                                }
                            }
                        }
                        isRecording -> {
                            LiveWaveformCanvas(
                                amplitudes = amplitudes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IdleWaveformGraphic(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tap record to start",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val totalSeconds = elapsedMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val formattedTime = if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isRecording -> MaterialTheme.colorScheme.outlineVariant
                            isPaused -> MaterialTheme.colorScheme.outline
                            else -> Color(0xFFE53935)
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    letterSpacing = (-0.5).sp,
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AudioRecordControls(
            isRecording = isRecording,
            isPaused = isPaused,
            onStartRecording = onStartRecording,
            onPauseToggle = onPauseToggle,
            onStop = onStop,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun IdleWaveformGraphic(
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val heights = listOf(
        0.18f, 0.28f, 0.42f, 0.65f, 0.9f, 0.72f, 0.55f, 0.85f, 1.0f,
        0.85f, 0.55f, 0.72f, 0.9f, 0.65f, 0.42f, 0.28f, 0.18f
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barWidth = 3.5.dp.toPx()
        val totalBars = heights.size
        val step = width / totalBars.toFloat()

        for (i in heights.indices) {
            val barHeight = (heights[i] * height).coerceAtLeast(4.dp.toPx())
            val x = (i * step) + (step / 2f)
            val top = centerY - (barHeight / 2f)

            drawLine(
                color = barColor,
                start = Offset(x, top),
                end = Offset(x, top + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun LiveWaveformCanvas(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.onSurface
    val dotColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barWidth = 4.dp.toPx()
        val barSpacing = 4.dp.toPx()
        val step = barWidth + barSpacing
        val maxBars = (width / step).toInt().coerceAtLeast(1)

        val visibleAmplitudes = amplitudes.takeLast(maxBars)
        val startX = width - (visibleAmplitudes.size * step)

        for (i in visibleAmplitudes.indices) {
            val amp = visibleAmplitudes[i].coerceIn(0.05f, 1f)
            val barHeight = (amp * (height * 0.85f)).coerceAtLeast(6.dp.toPx())
            val x = startX + (i * step)
            val top = centerY - (barHeight / 2f)

            drawLine(
                color = barColor,
                start = Offset(x, top),
                end = Offset(x, top + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }

        val dotRadius = 4.dp.toPx()
        val dotX = (startX + (visibleAmplitudes.size * step)).coerceAtMost(width - dotRadius)
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(dotX, centerY)
        )
    }
}

