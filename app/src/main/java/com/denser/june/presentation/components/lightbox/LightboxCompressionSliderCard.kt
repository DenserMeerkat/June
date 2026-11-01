package com.denser.june.presentation.components.lightbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.media.compress.model.CompressionPreset
import kotlin.math.roundToInt

@Composable
fun LightboxCompressionSliderCard(
    selectedPreset: CompressionPreset,
    isDarkTheme: Boolean,
    onPresetChanged: (CompressionPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) Color.Black.copy(alpha = 0.75f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Smaller size",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Higher Quality",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = selectedPreset.stepIndex.toFloat(),
                onValueChange = { pos ->
                    val step = pos.roundToInt().coerceIn(0, 3)
                    val newPreset = CompressionPreset.fromStepIndex(step)
                    if (newPreset != selectedPreset) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPresetChanged(newPreset)
                    }
                },
                valueRange = 0f..3f,
                steps = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
