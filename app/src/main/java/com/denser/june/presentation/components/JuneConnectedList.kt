package com.denser.june.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*

val JuneConnectedListShape = RoundedCornerShape(28.dp)
val JuneConnectedItemShape = RoundedCornerShape(12.dp)

@Composable
fun JuneConnectedList(
    modifier: Modifier = Modifier,
    shape: Shape = JuneConnectedListShape,
    spacing: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.clip(shape),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
fun <T> JuneConnectedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    shape: Shape = JuneConnectedListShape,
    spacing: Dp = 2.dp,
    itemContent: @Composable ColumnScope.(index: Int, count: Int, item: T) -> Unit
) {
    Column(
        modifier = modifier.clip(shape),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items.forEachIndexed { index, item ->
            itemContent(index, items.size, item)
        }
    }
}

@Composable
fun JuneConnectedItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    index: Int? = null,
    count: Int? = null,
    pressedRadius: Dp = 28.dp,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isClickable = onClick != null && enabled

    val defaultBaseShape = remember(index, count, shape) {
        if (shape != null) {
            shape
        } else if (index != null && count != null) {
            when {
                count == 1 -> RoundedCornerShape(28.dp)
                index == 0 -> RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
                index == count - 1 -> RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
                else -> RoundedCornerShape(8.dp)
            }
        } else {
            JuneConnectedItemShape
        }
    }

    val pressProgress by animateFloatAsState(
        targetValue = if (isClickable && isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "juneConnectedItemCornerAnim"
    )

    val currentShape = if (isClickable && defaultBaseShape is CornerBasedShape) {
        val density = LocalDensity.current
        val dummySize = androidx.compose.ui.geometry.Size(1000f, 1000f)
        val baseTopStart = (defaultBaseShape.topStart.toPx(dummySize, density) / density.density).dp
        val baseTopEnd = (defaultBaseShape.topEnd.toPx(dummySize, density) / density.density).dp
        val baseBottomStart = (defaultBaseShape.bottomStart.toPx(dummySize, density) / density.density).dp
        val baseBottomEnd = (defaultBaseShape.bottomEnd.toPx(dummySize, density) / density.density).dp

        RoundedCornerShape(
            topStart = lerp(baseTopStart, pressedRadius, pressProgress),
            topEnd = lerp(baseTopEnd, pressedRadius, pressProgress),
            bottomStart = lerp(baseBottomStart, pressedRadius, pressProgress),
            bottomEnd = lerp(baseBottomEnd, pressedRadius, pressProgress)
        )
    } else {
        defaultBaseShape
    }

    Surface(
        color = containerColor.copy(alpha = if (enabled) 1f else 0.5f),
        shape = currentShape,
        modifier = modifier
            .fillMaxWidth()
            .clip(currentShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        enabled = enabled,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingContent != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leadingContent()
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (trailingContent != null) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingContent()
                    }
                }
            }
            content?.invoke(this)
        }
    }
}
