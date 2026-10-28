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
import com.denser.june.presentation.screens.settings.screens.lock.PasswordLockContent
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.runtime.rememberCoroutineScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.pro.LocalIsPro
import com.denser.june.presentation.pro.LocalProSheetController
import kotlinx.coroutines.launch

private enum class PasswordSetupStep { CREATE, CONFIRM }

@Composable
fun PasswordSetupScreen(forVault: Boolean = false) {
    val settingsVM: SettingsVM = koinViewModel()
    val onAction = settingsVM::onAction
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val coroutineScope = rememberCoroutineScope()
    val navigator = koinInject<AppNavigator>()
    val isPro = LocalIsPro.current
    val proSheetController = LocalProSheetController.current
    var step by remember { mutableStateOf(PasswordSetupStep.CREATE) }
    var firstPassword by remember { mutableStateOf("") }
    var currentPasswordInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val title = when (step) {
        PasswordSetupStep.CREATE -> stringResource(R.string.create_password)
        PasswordSetupStep.CONFIRM -> stringResource(R.string.confirm_password)
    }

    val saveAndCompletePassword = { password: String ->
        val hash = SecurityUtils.hashPassword(password)
        if (forVault) {
            coroutineScope.launch {
                privacyPreferences.updateVaultPasswordHash(hash)
            }
            navigator.navigateTo(
                route = Route.RecoverySetup(LockType.PASSWORD, forVault = true),
                popUpToRoute = Route.PasswordSetup(forVault = true),
                inclusive = true
            )
        } else {
            onAction(SettingsAction.UpdatePasswordHash(hash))
            navigator.navigateTo(
                route = Route.RecoverySetup(LockType.PASSWORD, forVault = false),
                popUpToRoute = Route.PasswordSetup(forVault = false),
                inclusive = true
            )
        }
    }

    val submitPassword = { password: String ->
        when (step) {
            PasswordSetupStep.CREATE -> {
                firstPassword = password
                currentPasswordInput = ""
                step = PasswordSetupStep.CONFIRM
                isError = false
            }

            PasswordSetupStep.CONFIRM -> {
                if (password == firstPassword) {
                    if (!isPro) {
                        proSheetController.show {
                            saveAndCompletePassword(password)
                        }
                    } else {
                        saveAndCompletePassword(password)
                    }
                } else {
                    isError = true
                    currentPasswordInput = ""
                }
            }
        }
    }

    BackHandler(enabled = step == PasswordSetupStep.CONFIRM) {
        step = PasswordSetupStep.CREATE
        currentPasswordInput = firstPassword
        isError = false
    }

    PasswordLockContent(
        title = title,
        isError = isError,
        errorText = stringResource(R.string.does_not_match),
        onErrorDismissed = { isError = false },
        onPasswordChange = { currentPasswordInput = it },
        onBackClick = {
            if (step == PasswordSetupStep.CONFIRM) {
                step = PasswordSetupStep.CREATE
                currentPasswordInput = firstPassword
                isError = false
            } else {
                navigator.navigateBack()
            }
        },
        bottomBar = {
            LockBottomBar(
                onLeftClick = if (step == PasswordSetupStep.CONFIRM) {
                    {
                        step = PasswordSetupStep.CREATE
                        currentPasswordInput = firstPassword
                        isError = false
                    }
                } else null,
                leftIcon = R.drawable.arrow_back_24px,
                leftText = stringResource(R.string.back),
                onRightClick = {
                    submitPassword(currentPasswordInput)
                },
                rightEnabled = currentPasswordInput.isNotBlank(),
                rightText = if (step == PasswordSetupStep.CONFIRM) stringResource(R.string.confirm) else stringResource(R.string.next),
                rightIcon = R.drawable.start_24px
            )
        },
        onPasswordSubmitted = { password ->
            submitPassword(password)
        }
    )
}
