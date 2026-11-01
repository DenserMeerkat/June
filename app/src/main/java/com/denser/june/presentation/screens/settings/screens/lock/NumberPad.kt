package com.denser.june.presentation.screens.settings.screens.lock

import android.view.SoundEffectConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.denser.june.core.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val DELAY_WAVE_STAGGER = 30L

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberCircleToSquareMorphShape(progress: Float): Shape {
    val morph = remember { Morph(start = MaterialShapes.Circle, end = MaterialShapes.Square) }
    return remember(morph, progress) { MorphShape(morph, progress) }
}

@Composable
fun NumberPad(
    pinLength: Int,
    waveTrigger: Int,
    buttonSize: Dp,
    horizontalGap: Dp,
    verticalGap: Dp,
    modifier: Modifier = Modifier,
    onForgotPin: (() -> Unit)? = null,
    onNextClick: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    isNextConfirm: Boolean = false,
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
                    PadButton(
                        number = number.toString(),
                        scale = buttonScales[number].value,
                        buttonSize = buttonSize,
                        onClick = { onNumberClick(number.toString()) }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(horizontalGap)) {
            PadButton(
                buttonSize = buttonSize,
                enabled = pinLength > 0,
                onClick = onDeleteClick,
                onLongClick = onClearAll,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.backspace_24px_fill),
                        contentDescription = stringResource(R.string.action_delete),
                        modifier = Modifier.size(buttonSize * 0.42f)
                    )
                }
            )

            PadButton(
                number = "0",
                scale = buttonScales[0].value,
                buttonSize = buttonSize,
                onClick = { onNumberClick("0") }
            )

            if (onNextClick != null) {
                PadButton(
                    buttonSize = buttonSize,
                    enabled = nextEnabled,
                    onClick = onNextClick,
                    icon = {
                        Icon(
                            painter = painterResource(
                                R.drawable.start_24px
                            ),
                            contentDescription = stringResource(
                                if (isNextConfirm) R.string.action_confirm else R.string.next
                            ),
                            modifier = Modifier.size(buttonSize * 0.42f)
                        )
                    }
                )
            } else if (onForgotPin != null) {
                PadButton(
                    buttonSize = buttonSize,
                    onClick = onForgotPin,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.passkey_24px),
                            contentDescription = stringResource(R.string.forgot_lock),
                            modifier = Modifier.size(buttonSize * 0.42f)
                        )
                    }
                )
            } else {
                PadButton(
                    buttonSize = buttonSize,
                    enabled = false,
                    onClick = {},
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.start_24px),
                            contentDescription = stringResource(R.string.next),
                            modifier = Modifier.size(buttonSize * 0.42f)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PadButton(
    buttonSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: String? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    scale: Float = 1f,
    initialColor: Color = if (number != null) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    },
    activeColor: Color = if (number != null) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        initialColor
    },
    icon: (@Composable () -> Unit)? = null
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var tapTrigger by remember { mutableIntStateOf(0) }
    var isTapAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(tapTrigger) {
        if (tapTrigger > 0) {
            isTapAnimating = true
            delay(160.milliseconds)
            isTapAnimating = false
        }
    }

    val isActive = isPressed || isTapAnimating

    val morphProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "morph_progress"
    )

    val dynamicShape = rememberCircleToSquareMorphShape(morphProgress)

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) activeColor else initialColor,
        animationSpec = tween(durationMillis = 150),
        label = "bg_color"
    )

    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.3f,
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .size(buttonSize)
            .scale(scale)
            .alpha(alpha)
            .clip(dynamicShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    tapTrigger++
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onClick()
                },
                onLongClick = onLongClick
            ),
        shape = dynamicShape,
        color = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (number != null) {
                val fontScale by animateFloatAsState(
                    targetValue = if (isActive) 1.18f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "font_scale"
                )
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = if (buttonSize < 72.dp) 22.sp else 28.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                    modifier = Modifier.scale(fontScale)
                )
            } else {
                icon?.invoke()
            }
        }
    }
}
