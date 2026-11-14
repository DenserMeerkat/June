package com.denser.june.presentation.screens.settings.screens

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.screens.settings.screens.lock.LockGate
import com.denser.june.presentation.screens.settings.screens.lock.LockTarget
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.screens.lock.LockSessionVM
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VaultLockSettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state = settingsVM.state.collectAsStateWithLifecycle().value
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val navigator = koinInject<AppNavigator>()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lockSessionVM = koinInject<LockSessionVM>()
    val isVaultUnlocked by lockSessionVM.isVaultUnlocked.collectAsStateWithLifecycle()

    val useCustomVaultLock by privacyPreferences.getVaultUseCustomLockFlow().collectAsStateWithLifecycle(initialValue = false)
    val vaultLockType by privacyPreferences.getVaultLockTypeFlow().collectAsStateWithLifecycle(initialValue = null)
    val isAppLockEnabled by privacyPreferences.getAppLockFlow().collectAsStateWithLifecycle(initialValue = false)
    val isVaultProtected = (useCustomVaultLock && vaultLockType != null) || (isAppLockEnabled == true)

    var keepUnlocked by remember { mutableStateOf(false) }
    var localUnlocked by remember { mutableStateOf(isVaultUnlocked) }

    LaunchedEffect(isVaultUnlocked) {
        if (isVaultUnlocked) {
            localUnlocked = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!keepUnlocked && isVaultProtected) {
                lockSessionVM.lockVault()
            }
        }
    }

    if (isVaultProtected && !localUnlocked) {
        LockGate(
            target = LockTarget.VAULT,
            onUnlocked = {
                lockSessionVM.unlockVault()
                localUnlocked = true
            },
            onDismiss = { navigator.navigateBack() }
        )
        return
    }

    val appLockMsg = stringResource(R.string.app_lock)
    val lockSetSuccessMsg = stringResource(R.string.lock_set_successfully)

    val vaultPinHash by privacyPreferences.getVaultPinHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val vaultPasswordHash by privacyPreferences.getVaultPasswordHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val vaultPatternHash by privacyPreferences.getVaultPatternHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val vaultSecurityQuestion by privacyPreferences.getVaultSecurityQuestionFlow().collectAsStateWithLifecycle(initialValue = null)

    val defaultAppLockSubtitle = if (state.isAppLockEnabled) {
        when (state.lockType) {
            LockType.BIOMETRIC -> stringResource(R.string.same_as_screen_lock)
            LockType.PIN -> stringResource(R.string.custom_pin)
            LockType.PASSWORD -> stringResource(R.string.custom_password)
            LockType.PATTERN -> stringResource(R.string.custom_pattern)
        }
    } else {
        stringResource(R.string.no_lock)
    }

    LockMethodContent(
        title = stringResource(R.string.vault),
        description = stringResource(R.string.vault_desc),
        activeLockType = if (useCustomVaultLock) vaultLockType else null,
        hasActiveCredentials = useCustomVaultLock && vaultLockType != null && vaultLockType != LockType.BIOMETRIC,
        pinHash = vaultPinHash,
        passwordHash = vaultPasswordHash,
        patternHash = vaultPatternHash,
        securityQuestion = vaultSecurityQuestion,
        primaryOption = PrimaryLockOption(
            title = stringResource(R.string.app_lock),
            subtitle = defaultAppLockSubtitle,
            isSelected = !useCustomVaultLock || vaultLockType == null,
            onSelect = {
                coroutineScope.launch {
                    privacyPreferences.clearVaultCredentialsAndDisableCustomLock()
                }
                android.widget.Toast.makeText(context, appLockMsg, android.widget.Toast.LENGTH_SHORT).show()
            }
        ),
        onBackClick = { navigator.navigateBack() },
        onSelectBiometric = {
            coroutineScope.launch {
                privacyPreferences.setVaultCustomLock(
                    type = LockType.BIOMETRIC,
                    pinHash = null,
                    passwordHash = null,
                    patternHash = null,
                    securityQuestion = null,
                    securityAnswerHash = null
                )
            }
            android.widget.Toast.makeText(context, lockSetSuccessMsg, android.widget.Toast.LENGTH_SHORT).show()
        },
        onSelectPin = {
            keepUnlocked = true
            navigator.navigateTo(Route.PinSetup(forVault = true))
        },
        onSelectPassword = {
            keepUnlocked = true
            navigator.navigateTo(Route.PasswordSetup(forVault = true))
        },
        onSelectPattern = {
            keepUnlocked = true
            navigator.navigateTo(Route.PatternSetup(forVault = true))
        },
        onNavigateToRecovery = { lockType ->
            keepUnlocked = true
            navigator.navigateTo(Route.RecoverySetup(lockType, forVault = true))
        }
    )
}
