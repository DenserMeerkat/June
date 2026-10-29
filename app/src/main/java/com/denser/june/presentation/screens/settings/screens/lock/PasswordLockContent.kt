package com.denser.june.presentation.screens.settings.screens.lock
import com.denser.june.presentation.components.JuneBackButton

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneSecureTextField

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PasswordLockContent(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.enter_password),
    isError: Boolean = false,
    errorText: String = stringResource(R.string.wrong_pin_try_again),
    autoFocus: Boolean = true,
    onErrorDismissed: () -> Unit = {},
    onForgotPassword: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onPasswordChange: ((String) -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    onPasswordSubmitted: (String) -> Unit
) {
    val passwordState = rememberTextFieldState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    val shakeOffset = rememberShakeOffset()
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(passwordState) {
        snapshotFlow { passwordState.text.toString() }
            .collect { newText ->
                onPasswordChange?.invoke(newText)
                if (isError && newText.isNotEmpty()) {
                    onErrorDismissed()
                }
            }
    }

    LaunchedEffect(title) {
        passwordState.clearText()
    }

    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            shakeOffset.animateLockShake()
            passwordState.clearText()
            onErrorDismissed()
        }
    }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    val submitPassword = {
        val current = passwordState.text.toString()
        if (current.isNotBlank()) {
            keyboardController?.hide()
            passwordState.clearText()
            onPasswordSubmitted(current)
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
        val topSpacing = if (isSmallScreen) 32.dp else 64.dp
        val iconSpacing = if (isSmallScreen) 12.dp else 20.dp
        val headerToInputSpacing = if (isSmallScreen) 24.dp else 36.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(topSpacing))

            LockHeader(
                title = title,
                isError = isError,
                errorText = errorText,
                iconSpacing = iconSpacing
            )

            Spacer(modifier = Modifier.height(headerToInputSpacing))

            val animatedCornerRadius by animateDpAsState(
                targetValue = if (isFocused) 20.dp else 28.dp,
                animationSpec = spring(dampingRatio = 0.75f),
                label = "password_corner_radius"
            )
            val containerShape = RoundedCornerShape(animatedCornerRadius)

            Column(
                modifier = Modifier
                    .offset(x = shakeOffset.value.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                JuneSecureTextField(
                    state = passwordState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = stringResource(R.string.enter_password),
                    placeholderStyle = MaterialTheme.typography.bodyLarge,
                    isError = isError,
                    textObfuscationMode = if (isPasswordVisible) {
                        TextObfuscationMode.Visible
                    } else {
                        TextObfuscationMode.Hidden
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    onKeyboardAction = {
                        submitPassword()
                    },
                    shape = containerShape,
                    interactionSource = interactionSource,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isPasswordVisible) R.drawable.visibility_off_24px else R.drawable.visibility_24px
                                ),
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
        ) {
            if (bottomBar != null) {
                bottomBar()
            } else {
                LockBottomBar(
                    onLeftClick = onForgotPassword,
                    leftIcon = R.drawable.passkey_24px,
                    leftText = null,
                    onRightClick = submitPassword,
                    rightEnabled = passwordState.text.isNotBlank(),
                    rightText = null,
                    rightIcon = R.drawable.start_24px
                )
            }
        }
    }
}
