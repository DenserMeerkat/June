package com.denser.june.presentation.screens.settings.screens.lock
import com.denser.june.presentation.components.JuneBackButton

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import kotlinx.coroutines.delay

private const val DELAY_AUTO_SUBMIT = 100L

@Composable
fun PinLockContent(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.enter_pin),
    isError: Boolean = false,
    errorText: String = stringResource(R.string.wrong_pin_try_again),
    maxPinLength: Int = 6,
    onErrorDismissed: () -> Unit = {},
    onForgotPin: (() -> Unit)? = null,
    onNextClick: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    isNextConfirm: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onPinChange: ((String) -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    onPinSubmitted: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var waveTrigger by remember { mutableIntStateOf(0) }
    val shakeOffset = rememberShakeOffset()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(title) {
        pin = ""
        onPinChange?.invoke("")
    }

    LaunchedEffect(isError) {
        if (isError) {
            pin = ""
            onPinChange?.invoke("")
            waveTrigger++
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            shakeOffset.animateLockShake()
            onErrorDismissed()
        }
    }

    LaunchedEffect(pin) {
        if (bottomBar == null && onNextClick == null && pin.length == maxPinLength) {
            delay(DELAY_AUTO_SUBMIT)
            val submittedPin = pin
            pin = ""
            onPinChange?.invoke("")
            onPinSubmitted(submittedPin)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isSmallScreen = maxHeight < 640.dp
        val topSpacing = if (isSmallScreen) 16.dp else 40.dp
        val iconSpacing = if (isSmallScreen) 12.dp else 24.dp
        val headerToIndicatorSpacing = if (isSmallScreen) 16.dp else 36.dp
        val indicatorToInputSpacing = if (isSmallScreen) 16.dp else 36.dp
        val buttonSize = if (maxWidth < 360.dp) 60.dp else if (maxWidth < 400.dp) 68.dp else 74.dp
        val horizontalGap = if (maxWidth < 360.dp) 6.dp else if (maxWidth < 400.dp) 8.dp else 12.dp
        val verticalGap = if (isSmallScreen) 4.dp else 8.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(topSpacing))

            LockHeader(
                title = title,
                isError = isError,
                errorText = errorText,
                iconSpacing = iconSpacing
            )

            Spacer(modifier = Modifier.height(headerToIndicatorSpacing))

            Column(
                modifier = Modifier
                    .offset(x = shakeOffset.value.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PinIndicatorRow(
                    pinLength = pin.length,
                    maxPinLength = maxPinLength,
                    isError = isError
                )

                Spacer(modifier = Modifier.height(indicatorToInputSpacing))

                NumberPad(
                    pinLength = pin.length,
                    waveTrigger = waveTrigger,
                    buttonSize = buttonSize,
                    horizontalGap = horizontalGap,
                    verticalGap = verticalGap,
                    onForgotPin = onForgotPin,
                    onNextClick = onNextClick,
                    nextEnabled = nextEnabled,
                    isNextConfirm = isNextConfirm,
                    onNumberClick = {
                        if (pin.length < maxPinLength) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pin += it
                            onPinChange?.invoke(pin)
                            if (isError) onErrorDismissed()
                        }
                    },
                    onDeleteClick = {
                        if (pin.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pin = pin.dropLast(1)
                            onPinChange?.invoke(pin)
                            if (isError) onErrorDismissed()
                        }
                    },
                    onClearAll = {
                        if (pin.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pin = ""
                            onPinChange?.invoke("")
                            if (isError) onErrorDismissed()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(topSpacing))
            Spacer(modifier = Modifier.weight(1f, fill = false))
        }

        if (onBackClick != null) {
            JuneBackButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp)
                    .align(Alignment.TopStart)
            )
        }

        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                bottomBar()
            }
        }
    }
}
