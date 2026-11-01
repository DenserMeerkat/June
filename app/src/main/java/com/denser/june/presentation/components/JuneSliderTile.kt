package com.denser.june.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneSliderTile(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueDisplay: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    trackBrush: Brush? = null,
    thumbColor: Color? = null,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (trackBrush != null) 6.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (trackBrush != null) FontWeight.Medium else FontWeight.Normal,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (valueDisplay != null) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (trackBrush != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        if (trackBrush != null) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                val totalWidthPx = constraints.maxWidth.toFloat()
                val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
                    .coerceIn(0f, 1f)

                val trackHeight = 12.dp
                val thumbWidth = 8.dp
                val thumbHeight = 28.dp
                val gapDp = 4.dp
                val gapPx = with(LocalDensity.current) { gapDp.toPx() }

                val thumbHalf = with(LocalDensity.current) { (thumbWidth / 2).toPx() }
                val availableTravel = (totalWidthPx - 2 * thumbHalf).coerceAtLeast(0f)
                val thumbCenterPx = thumbHalf + fraction * availableTravel
                val leftSegmentEndPx = (thumbCenterPx - thumbHalf - gapPx).coerceAtLeast(0f)
                val rightSegmentStartPx = (thumbCenterPx + thumbHalf + gapPx).coerceAtMost(totalWidthPx)

                val innerBreakCornerPx = with(LocalDensity.current) { 4.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                ) {
                    val h = size.height
                    val outerRadius = h / 2f
                    val innerRadius = innerBreakCornerPx

                    if (leftSegmentEndPx > 0f) {
                        val leftW = leftSegmentEndPx.coerceAtMost(size.width)
                        val leftPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = 0f,
                                    top = 0f,
                                    right = leftW,
                                    bottom = h,
                                    topLeftCornerRadius = CornerRadius(outerRadius, outerRadius),
                                    bottomLeftCornerRadius = CornerRadius(outerRadius, outerRadius),
                                    topRightCornerRadius = CornerRadius(innerRadius, innerRadius),
                                    bottomRightCornerRadius = CornerRadius(innerRadius, innerRadius)
                                )
                            )
                        }
                        drawPath(path = leftPath, brush = trackBrush)
                    }

                    if (rightSegmentStartPx < size.width) {
                        val rightPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = rightSegmentStartPx,
                                    top = 0f,
                                    right = size.width,
                                    bottom = h,
                                    topLeftCornerRadius = CornerRadius(innerRadius, innerRadius),
                                    bottomLeftCornerRadius = CornerRadius(innerRadius, innerRadius),
                                    topRightCornerRadius = CornerRadius(outerRadius, outerRadius),
                                    bottomRightCornerRadius = CornerRadius(outerRadius, outerRadius)
                                )
                            )
                        }
                        drawPath(path = rightPath, brush = trackBrush)
                    }
                }

                val activeThumbColor = thumbColor ?: MaterialTheme.colorScheme.primary
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .width(thumbWidth)
                                .height(thumbHeight)
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(thumbWidth / 2))
                                .clip(RoundedCornerShape(thumbWidth / 2))
                                .background(if (enabled) activeThumbColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                        )
                    },
                    track = {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                )
            }
        } else {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled
            )
        }
    }
}
