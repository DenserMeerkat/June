package com.denser.june.presentation.screens.settings.screens.lock
import com.denser.june.presentation.components.JuneBackButton

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.utils.SecurityUtils
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

enum class LockTarget {
    APP,
    VAULT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockGate(
    target: LockTarget = LockTarget.VAULT,
    title: String? = null,
    showBackButton: Boolean = true,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val coroutineScope = rememberCoroutineScope()

    val isVaultTarget = target == LockTarget.VAULT
    val isAppLockEnabled by privacyPreferences.getAppLockFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    val useCustomVaultLock by privacyPreferences.getVaultUseCustomLockFlow()
        .collectAsStateWithLifecycle(initialValue = false)
    val vaultLockType by privacyPreferences.getVaultLockTypeFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    val globalLockType by privacyPreferences.getLockTypeFlow()
        .collectAsStateWithLifecycle(initialValue = null)

    val isVaultCustom = isVaultTarget && useCustomVaultLock && vaultLockType != null
    val effectiveLockType = if (isVaultCustom) vaultLockType else globalLockType

    val isLockActive = if (isVaultCustom) {
        true
    } else {
        isAppLockEnabled == true
    }

    val vaultPinHash by privacyPreferences.getVaultPinHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val globalPinHash by privacyPreferences.getPinHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val effectivePinHash = if (isVaultCustom) vaultPinHash else globalPinHash

    val vaultPasswordHash by privacyPreferences.getVaultPasswordHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val globalPasswordHash by privacyPreferences.getPasswordHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val effectivePasswordHash = if (isVaultCustom) vaultPasswordHash else globalPasswordHash

    val vaultPatternHash by privacyPreferences.getVaultPatternHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val globalPatternHash by privacyPreferences.getPatternHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val effectivePatternHash = if (isVaultCustom) vaultPatternHash else globalPatternHash

    val vaultSecurityQuestion by privacyPreferences.getVaultSecurityQuestionFlow().collectAsStateWithLifecycle(initialValue = null)
    val globalSecurityQuestion by privacyPreferences.getSecurityQuestionFlow().collectAsStateWithLifecycle(initialValue = null)
    val effectiveSecurityQuestion = if (isVaultCustom) vaultSecurityQuestion else globalSecurityQuestion

    val vaultSecurityAnswerHash by privacyPreferences.getVaultSecurityAnswerHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val globalSecurityAnswerHash by privacyPreferences.getSecurityAnswerHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val effectiveSecurityAnswerHash = if (isVaultCustom) vaultSecurityAnswerHash else globalSecurityAnswerHash

    var isError by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(isLockActive, isAppLockEnabled) {
        if (isAppLockEnabled != null && !isLockActive) {
            onUnlocked()
        }
    }

    BackHandler {
        if (showRecovery) {
            showRecovery = false
        } else {
            onDismiss()
        }
    }

    if (isAppLockEnabled == null || !isLockActive) {
        return
    }

    val currentLockType = effectiveLockType ?: return

    if (showRecovery && effectiveSecurityQuestion != null && effectiveSecurityAnswerHash != null) {
        LockRecoveryContent(
            question = effectiveSecurityQuestion,
            storedAnswerHash = effectiveSecurityAnswerHash,
            onBackClick = { showRecovery = false },
            onResetSuccess = {
                coroutineScope.launch {
                    if (isVaultCustom) {
                        privacyPreferences.clearVaultCredentialsAndDisableCustomLock()
                    } else {
                        privacyPreferences.clearAppLockCredentialsAndDisableLock()
                    }
                    showRecovery = false
                    onUnlocked()
                }
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentLockType) {
            LockType.BIOMETRIC -> {
                val promptTitle = title
                    ?: if (isVaultTarget) {
                        stringResource(R.string.unlock_vault)
                    } else {
                        stringResource(R.string.unlock_june)
                    }

                val activity = remember(context) { context.findFragmentActivity() }

                fun triggerBiometrics() {
                    if (activity == null) return
                    val biometricManager = BiometricManager.from(activity)
                    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
                        val executor = ContextCompat.getMainExecutor(activity)
                        val biometricPrompt = BiometricPrompt(
                            activity,
                            executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    onUnlocked()
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                    ) {
                                        onDismiss()
                                    }
                                }
                            }
                        )
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(promptTitle)
                            .setAllowedAuthenticators(authenticators)
                            .build()
                        biometricPrompt.authenticate(promptInfo)
                    }
                }

                LaunchedEffect(activity) {
                    triggerBiometrics()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            triggerBiometrics()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.6f))
                    AppIconDisplay(size = 112.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.locked),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(2.4f))
                }
            }

            LockType.PIN -> {
                PinLockContent(
                    title = title ?: stringResource(R.string.enter_pin),
                    isError = isError,
                    onErrorDismissed = { isError = false },
                    onForgotPin = if (effectiveSecurityQuestion != null) { { showRecovery = true } } else null,
                    onPinSubmitted = { pin ->
                        val inputHash = SecurityUtils.hashPin(pin)
                        if (effectivePinHash != null && inputHash == effectivePinHash) {
                            onUnlocked()
                        } else {
                            isError = true
                        }
                    }
                )
            }

            LockType.PASSWORD -> {
                PasswordLockContent(
                    title = title ?: stringResource(R.string.enter_password),
                    isError = isError,
                    onErrorDismissed = { isError = false },
                    onForgotPassword = if (effectiveSecurityQuestion != null) { { showRecovery = true } } else null,
                    onPasswordSubmitted = { pass ->
                        val inputHash = SecurityUtils.hashPassword(pass)
                        if (effectivePasswordHash != null && inputHash == effectivePasswordHash) {
                            onUnlocked()
                        } else {
                            isError = true
                        }
                    }
                )
            }

            LockType.PATTERN -> {
                PatternLockContent(
                    title = title ?: stringResource(R.string.draw_pattern),
                    isError = isError,
                    onErrorDismissed = { isError = false },
                    onForgotPattern = if (effectiveSecurityQuestion != null) { { showRecovery = true } } else null,
                    onPatternSubmitted = { pattern ->
                        val inputHash = SecurityUtils.hashPattern(pattern)
                        if (effectivePatternHash != null && inputHash == effectivePatternHash) {
                            onUnlocked()
                        } else {
                            isError = true
                        }
                    }
                )
            }
        }

        if (showBackButton) {
            JuneBackButton(
                onClick = onDismiss,
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
    }
}
