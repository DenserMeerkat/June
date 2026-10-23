package com.denser.june.presentation.components.lock

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val DELAY_WAVE_STAGGER = 30L

@Composable
fun NumberPad(
    pinLength: Int,
    waveTrigger: Int,
    buttonSize: Dp,
    horizontalGap: Dp,
    verticalGap: Dp,
    modifier: Modifier = Modifier,
    onForgotPin: (() -> Unit)? = null,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
) {
    val buttonScales = remember { List(10) { Animatable(1f) } }

    LaunchedEffect(waveTrigger) {
        if (waveTrigger > 0) {
            val sequence = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
            sequence.forEachIndexed { index, num ->
                launch {
                    delay((index * DELAY_WAVE_STAGGER).milliseconds)
                    buttonScales[num].animateTo(0.8f, tween(100))
                    buttonScales[num].animateTo(
                        1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(verticalGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        (1..9).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
                row.forEach { number ->
                    NumberButton(number.toString(), buttonScales[number].value, buttonSize, onNumberClick)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
            PadButton(
                onClick = onDeleteClick,
                onLongClick = onClearAll,
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = pinLength > 0,
                buttonSize = buttonSize
            ) {
                Icon(
                    painterResource(R.drawable.backspace_24px_fill),
                    contentDescription = "Delete",
                    modifier = Modifier.size(buttonSize * 0.45f)
                )
            }
            NumberButton("0", buttonScales[0].value, buttonSize, onNumberClick)

            if (onForgotPin != null) {
                PadButton(
                    onClick = onForgotPin,
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    buttonSize = buttonSize
                ) {
                    Icon(
                        painter = painterResource(R.drawable.passkey_24px),
                        contentDescription = "Forgot PIN",
                        modifier = Modifier.size(buttonSize * 0.45f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(buttonSize))
            }
        }
    }
}

@Composable
fun NumberButton(
    number: String,
    externalScale: Float,
    buttonSize: Dp,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PadButton(
        onClick = { onClick(number) },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        externalScale = externalScale,
        buttonSize = buttonSize,
        modifier = modifier
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontSize = if (buttonSize < 72.dp) 22.sp else 28.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    backgroundColor: Color,
    contentColor: Color,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    externalScale: Float = 1f,
    buttonSize: Dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "pressScale")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f, label = "alpha")

    Surface(
        modifier = modifier
            .size(buttonSize)
            .scale(pressScale * externalScale)
            .alpha(alpha)
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
