package com.denser.june.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.PinLockScreen
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.core.utils.SecurityUtils
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private enum class SetupStep { CREATE, CONFIRM }

@Composable
fun PinSetupScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val onAction = settingsVM::onAction
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
                            navigator.navigateTo(
                                route = Route.RecoverySetup,
                                popUpToRoute = Route.PinSetup,
                                inclusive = true
                            )
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