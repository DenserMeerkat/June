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
import com.denser.june.presentation.screens.settings.screens.lock.PatternLockContent
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.runtime.rememberCoroutineScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.plus.LocalIsPlus
import com.denser.june.presentation.plus.LocalPlusSheetController
import kotlinx.coroutines.launch

private enum class PatternSetupStep { CREATE, CONFIRM }

@Composable
fun PatternSetupScreen(forVault: Boolean = false) {
    val settingsVM: SettingsVM = koinViewModel()
    val onAction = settingsVM::onAction
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val coroutineScope = rememberCoroutineScope()
    val navigator = koinInject<AppNavigator>()
    val isPlus = LocalIsPlus.current
    val plusSheetController = LocalPlusSheetController.current
    var step by remember { mutableStateOf(PatternSetupStep.CREATE) }
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentPatternInput by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isError by remember { mutableStateOf(false) }

    val title = when (step) {
        PatternSetupStep.CREATE -> stringResource(R.string.draw_pattern)
        PatternSetupStep.CONFIRM -> stringResource(R.string.confirm_pattern)
    }

    val saveAndCompletePattern = { pattern: List<Int> ->
        val hash = SecurityUtils.hashPattern(pattern)
        if (forVault) {
            coroutineScope.launch {
                privacyPreferences.updateVaultPatternHash(hash)
            }
            navigator.navigateTo(
                route = Route.RecoverySetup(LockType.PATTERN, forVault = true),
                popUpToRoute = Route.PatternSetup(forVault = true),
                inclusive = true
            )
        } else {
            onAction(SettingsAction.UpdatePatternHash(hash))
            navigator.navigateTo(
                route = Route.RecoverySetup(LockType.PATTERN, forVault = false),
                popUpToRoute = Route.PatternSetup(forVault = false),
                inclusive = true
            )
        }
    }

    val submitPattern = { pattern: List<Int> ->
        when (step) {
            PatternSetupStep.CREATE -> {
                firstPattern = pattern
                currentPatternInput = emptyList()
                step = PatternSetupStep.CONFIRM
                isError = false
            }

            PatternSetupStep.CONFIRM -> {
                if (pattern == firstPattern) {
                    if (!isPlus) {
                        plusSheetController.show {
                            saveAndCompletePattern(pattern)
                        }
                    } else {
                        saveAndCompletePattern(pattern)
                    }
                } else {
                    isError = true
                    currentPatternInput = emptyList()
                }
            }
        }
    }

    BackHandler(enabled = step == PatternSetupStep.CONFIRM) {
        step = PatternSetupStep.CREATE
        currentPatternInput = firstPattern
        isError = false
    }

    PatternLockContent(
        title = title,
        isError = isError,
        errorText = stringResource(R.string.does_not_match),
        onErrorDismissed = { isError = false },
        onPatternChange = { currentPatternInput = it },
        onBackClick = {
            if (step == PatternSetupStep.CONFIRM) {
                step = PatternSetupStep.CREATE
                currentPatternInput = firstPattern
                isError = false
            } else {
                navigator.navigateBack()
            }
        },
        bottomBar = {
            LockBottomBar(
                onLeftClick = if (step == PatternSetupStep.CONFIRM) {
                    {
                        step = PatternSetupStep.CREATE
                        currentPatternInput = firstPattern
                        isError = false
                    }
                } else null,
                leftIcon = R.drawable.arrow_back_24px,
                leftText = stringResource(R.string.action_back),
                onRightClick = {
                    submitPattern(currentPatternInput)
                },
                rightEnabled = currentPatternInput.size >= 4,
                rightText = if (step == PatternSetupStep.CONFIRM) stringResource(R.string.action_confirm) else stringResource(R.string.next),
                rightIcon = R.drawable.start_24px
            )
        },
        onPatternSubmitted = { pattern ->
            submitPattern(pattern)
        }
    )
}
