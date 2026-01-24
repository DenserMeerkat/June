package com.denser.june.core.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.denser.june.core.domain.enums.LockType
import com.denser.june.core.navigation.AppNavigator
import com.denser.june.core.presentation.components.PinLockScreen
import com.denser.june.core.presentation.screens.settings.SettingsAction
import com.denser.june.core.utils.SecurityUtils
import org.koin.compose.koinInject

private enum class SetupStep { CREATE, CONFIRM }

@Composable
fun PinSetupScreen(
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()
    var step by remember { mutableStateOf(SetupStep.CREATE) }
    var firstPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val title = when (step) {
        SetupStep.CREATE -> "Create PIN"
        SetupStep.CONFIRM -> "Confirm PIN"
    }

    key(step) {
        PinLockScreen(
            title = title,
            isError = isError,
            onPinSubmitted = { pin ->
                when (step) {
                    SetupStep.CREATE -> {
                        firstPin = pin
                        step = SetupStep.CONFIRM
                        isError = false
                    }

                    SetupStep.CONFIRM -> {
                        if (pin == firstPin) {
                            val hash = SecurityUtils.hashPin(pin)
                            onAction(SettingsAction.UpdatePinHash(hash))
                            onAction(SettingsAction.UpdateLockType(LockType.PIN))
                            onAction(SettingsAction.OnAppLockToggle(true))
                            navigator.navigateBack()
                        } else {
                            isError = true
                            step = SetupStep.CREATE
                            firstPin = ""
                        }
                    }
                }
            }
        )
    }
}