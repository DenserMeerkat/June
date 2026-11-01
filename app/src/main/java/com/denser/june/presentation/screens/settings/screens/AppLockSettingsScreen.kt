package com.denser.june.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppLockSettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state = settingsVM.state.collectAsStateWithLifecycle().value
    val onAction = settingsVM::onAction
    val navigator = koinInject<AppNavigator>()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lockDisabledSuccessMsg = stringResource(R.string.lock_disabled_successfully)
    val lockSetSuccessMsg = stringResource(R.string.lock_set_successfully)

    LockMethodContent(
        title = stringResource(R.string.app_lock),
        description = stringResource(R.string.app_lock_desc),
        activeLockType = if (state.isAppLockEnabled) state.lockType else null,
        hasActiveCredentials = state.isAppLockEnabled && state.lockType != LockType.BIOMETRIC,
        pinHash = state.pinHash,
        passwordHash = state.passwordHash,
        patternHash = state.patternHash,
        securityQuestion = state.securityQuestion,
        onBackClick = { navigator.navigateBack() },
        onSelectBiometric = {
            onAction(SettingsAction.UpdateLockType(LockType.BIOMETRIC))
            onAction(SettingsAction.OnAppLockToggle(true))
            android.widget.Toast.makeText(context, lockSetSuccessMsg, android.widget.Toast.LENGTH_SHORT).show()
        },
        onSelectPin = { navigator.navigateTo(Route.PinSetup(forVault = false)) },
        onSelectPassword = { navigator.navigateTo(Route.PasswordSetup(forVault = false)) },
        onSelectPattern = { navigator.navigateTo(Route.PatternSetup(forVault = false)) },
        onSelectNone = {
            onAction(SettingsAction.OnAppLockToggle(false))
            android.widget.Toast.makeText(context, lockDisabledSuccessMsg, android.widget.Toast.LENGTH_SHORT).show()
        },
        onNavigateToRecovery = { lockType ->
            navigator.navigateTo(Route.RecoverySetup(lockType, forVault = false))
        }
    )
}
