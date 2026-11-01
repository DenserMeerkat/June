package com.denser.june.presentation.screens.settings.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.utils.SecurityUtils
import com.denser.june.presentation.screens.settings.screens.lock.LockBottomBar
import com.denser.june.presentation.screens.settings.screens.lock.PinLockContent
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.runtime.rememberCoroutineScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import kotlinx.coroutines.launch

private enum class SetupStep { CREATE, CONFIRM }

@Composable
fun PinSetupScreen(forVault: Boolean = false) {
    val settingsVM: SettingsVM = koinViewModel()
    val onAction = settingsVM::onAction
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val coroutineScope = rememberCoroutineScope()
    val navigator = koinInject<AppNavigator>()
    val isPlus = com.denser.june.presentation.plus.LocalIsPlus.current
    val plusSheetController = com.denser.june.presentation.plus.LocalPlusSheetController.current
    var step by remember { mutableStateOf(SetupStep.CREATE) }
    var firstPin by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val title = when (step) {
        SetupStep.CREATE -> stringResource(R.string.create_pin)
        SetupStep.CONFIRM -> stringResource(R.string.confirm_pin)
    }

    val saveAndCompletePin = { pin: String ->
        val hash = SecurityUtils.hashPin(pin)
        if (forVault) {
            coroutineScope.launch {
                privacyPreferences.updateVaultPinHash(hash)
            }
            navigator.navigateTo(
                route = Route.RecoverySetup(LockType.PIN, forVault = true),
                popUpToRoute = Route.PinSetup(forVault = true),
                inclusive = true
            )
        } else {
            onAction(SettingsAction.UpdatePinHash(hash))
            navigator.navigateTo(
                route = Route.RecoverySetup(LockType.PIN, forVault = false),
                popUpToRoute = Route.PinSetup(forVault = false),
                inclusive = true
            )
        }
    }

    val submitPin = { pin: String ->
        when (step) {
            SetupStep.CREATE -> {
                firstPin = pin
                currentPinInput = ""
                step = SetupStep.CONFIRM
                isError = false
            }

            SetupStep.CONFIRM -> {
                if (pin == firstPin) {
                    if (forVault && !isPlus) {
                        plusSheetController.show {
                            saveAndCompletePin(pin)
                        }
                    } else {
                        saveAndCompletePin(pin)
                    }
                } else {
                    isError = true
                    currentPinInput = ""
                }
            }
        }
    }

    BackHandler(enabled = step == SetupStep.CONFIRM) {
        step = SetupStep.CREATE
        currentPinInput = firstPin
        isError = false
    }

    PinLockContent(
        title = title,
        isError = isError,
        errorText = stringResource(R.string.does_not_match),
        onErrorDismissed = { isError = false },
        onPinChange = { currentPinInput = it },
        onNextClick = {
            submitPin(currentPinInput)
        },
        nextEnabled = currentPinInput.length == 6,
        isNextConfirm = step == SetupStep.CONFIRM,
        onBackClick = {
            if (step == SetupStep.CONFIRM) {
                step = SetupStep.CREATE
                currentPinInput = firstPin
                isError = false
            } else {
                navigator.navigateBack()
            }
        },
        bottomBar = {
            LockBottomBar(
                onLeftClick = if (step == SetupStep.CONFIRM) {
                    {
                        step = SetupStep.CREATE
                        currentPinInput = firstPin
                        isError = false
                    }
                } else null,
                leftIcon = R.drawable.arrow_back_24px,
                leftText = stringResource(R.string.action_back),
                onRightClick = {
                    submitPin(currentPinInput)
                },
                rightEnabled = currentPinInput.length == 6,
                rightText = if (step == SetupStep.CONFIRM) stringResource(R.string.action_confirm) else stringResource(R.string.next),
                rightIcon = R.drawable.start_24px
            )
        },
        onPinSubmitted = { pin ->
            submitPin(pin)
        }
    )
}
