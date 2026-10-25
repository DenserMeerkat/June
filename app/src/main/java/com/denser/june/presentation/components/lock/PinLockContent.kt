package com.denser.june.presentation.components.lock

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import kotlinx.coroutines.delay

private const val DELAY_AUTO_SUBMIT = 100L

@Composable
fun PinLockScreen(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.enter_pin),
    isError: Boolean = false,
    maxPinLength: Int = 6,
    onForgotPin: (() -> Unit)? = null,
    onPinSubmitted: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var waveTrigger by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isError) {
        if (isError) {
            pin = ""
            waveTrigger++
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == maxPinLength) {
            delay(DELAY_AUTO_SUBMIT)
            onPinSubmitted(pin)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val buttonSize = if (screenWidth < 360.dp) 64.dp else if (screenWidth < 400.dp) 72.dp else 80.dp
        val horizontalGap = if (screenWidth < 360.dp) 12.dp else if (screenWidth < 400.dp) 18.dp else 24.dp
        val verticalGap = if (screenHeight < 640.dp) 8.dp else 16.dp

        val spacerHeight1 = if (screenHeight < 640.dp) 12.dp else 24.dp
        val spacerHeight2 = if (screenHeight < 640.dp) 24.dp else 48.dp
        val middleSpacerHeight = if (screenHeight < 640.dp) 24.dp else 48.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(vertical = if (screenHeight < 640.dp) 24.dp else 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LockHeader(
                title = title,
                isError = isError,
                iconSpacing = spacerHeight1
            )

            Spacer(modifier = Modifier.height(spacerHeight2))

            PinIndicatorRow(pinLength = pin.length, maxPinLength = maxPinLength)

            Spacer(modifier = Modifier.height(middleSpacerHeight))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(verticalGap)
            ) {
                NumberPad(
                    pinLength = pin.length,
                    waveTrigger = waveTrigger,
                    buttonSize = buttonSize,
                    horizontalGap = horizontalGap,
                    verticalGap = verticalGap,
                    onForgotPin = onForgotPin,
                    onNumberClick = {
                        if (pin.length < maxPinLength) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pin += it
                        }
                    },
                    onDeleteClick = {
                        if (pin.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pin = pin.dropLast(1)
                        }
                    },
                    onClearAll = {
                        if (pin.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pin = ""
                        }
                    }
                )
            }
        }
    }
}