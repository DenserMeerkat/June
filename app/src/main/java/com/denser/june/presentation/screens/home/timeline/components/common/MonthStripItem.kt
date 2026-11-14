package com.denser.june.presentation.screens.home.timeline.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MonthStripItem(
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) -180f else 0f,
        label = "Chevron Rotation"
    )

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        ToggleButton(
            checked = isSelected,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
            shapes = ToggleButtonDefaults.shapes(
                shape = RoundedCornerShape(10.dp),
                pressedShape = RoundedCornerShape(18.dp),
                checkedShape = CircleShape
            ),
            colors = ToggleButtonDefaults.toggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = if (isSelected) 12.dp else 16.dp
            ),
            modifier = Modifier
                .height(36.dp)
                .widthIn(min = 48.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                VerticalDivider(
                    modifier = Modifier.height(18.dp),
                    color = contentColor.copy(alpha = 0.3f)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = contentColor,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

