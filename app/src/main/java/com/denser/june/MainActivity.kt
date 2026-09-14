package com.denser.june

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.SideEffect
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.components.PinLockScreen
import com.denser.june.presentation.components.PinRecoveryScreen
import com.denser.june.core.utils.SecurityUtils
import com.denser.june.presentation.JuneApp
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.utils.ExternalIntentProcessor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import org.koin.android.ext.android.inject
import com.denser.june.core.R
import androidx.core.graphics.drawable.toDrawable

enum class LockState {
    LOADING,
    LOCKED_BIOMETRIC,
    LOCKED_PIN,
    LOCKED_SECURITY_QUESTION,
    UNLOCKED
}

class MainActivity : AppCompatActivity() {

    companion object {
        private var activeActivity: WeakReference<MainActivity>? = null
    }

    private val privacyPreferences: PrivacyPreferences by inject()
    private val journalPreferences: JournalPreferences by inject()
    private val externalIntentProcessor: ExternalIntentProcessor by inject()
    private var lockState by mutableStateOf(LockState.LOADING)
    private var pendingRoute by mutableStateOf<Route?>(null)
    private var pendingIntentRoute: Route? = null

    private var isPinError by mutableStateOf(false)
    private var storedPinHash: String? = null
    private var storedSecurityQuestion: String? = null
    private var storedSecurityAnswerHash: String? = null

    private fun onUnlocked() {
        lockState = LockState.UNLOCKED
        pendingIntentRoute?.let { route ->
            pendingRoute = route
            pendingIntentRoute = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        lifecycleScope.launch {
            val route = externalIntentProcessor.processIntent(intent) ?: return@launch
            if (lockState == LockState.UNLOCKED) {
                pendingRoute = route
            } else {
                pendingIntentRoute = route
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        activeActivity?.get()?.let { previous ->
            if (previous != this && !previous.isFinishing) {
                previous.finish()
            }
        }
        activeActivity = WeakReference(this)

        splashScreen.setKeepOnScreenCondition { lockState == LockState.LOADING }
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        lifecycleScope.launch {
            val isLockEnabled = privacyPreferences.getAppLockFlow().first()
            val lockType = privacyPreferences.getLockTypeFlow().first()
            storedPinHash = privacyPreferences.getPinHashFlow().first()
            storedSecurityQuestion = privacyPreferences.getSecurityQuestionFlow().first()
            storedSecurityAnswerHash = privacyPreferences.getSecurityAnswerHashFlow().first()

            val isExternalIntent = intent.action == Intent.ACTION_VIEW ||
                    intent.action == Intent.ACTION_EDIT ||
                    intent.action == Intent.ACTION_SEND

            if (!isExternalIntent) {
                val targetRoute = when {
                    intent.getBooleanExtra("OPEN_NEW_NOTE", false) -> {
                        intent.removeExtra("OPEN_NEW_NOTE")
                        Route.Editor()
                    }

                    intent.getBooleanExtra("OPEN_SYNC_SETTINGS", false) -> {
                        intent.removeExtra("OPEN_SYNC_SETTINGS")
                        Route.SyncSettings
                    }

                    savedInstanceState == null && journalPreferences.alwaysOpenNewNote()
                        .first() -> {
                        Route.Editor()
                    }

                    else -> null
                }
                if (targetRoute != null) {
                    if (lockState == LockState.UNLOCKED) {
                        pendingRoute = targetRoute
                    } else {
                        pendingIntentRoute = targetRoute
                    }
                }
            }

            if (!isLockEnabled) {
                onUnlocked()
            } else {
                when (lockType) {
                    LockType.BIOMETRIC -> {
                        lockState = LockState.LOCKED_BIOMETRIC
                        checkBiometricAndAuthenticate()
                    }

                    LockType.PIN -> {
                        if (storedPinHash != null) {
                            lockState = LockState.LOCKED_PIN
                        } else {
                            onUnlocked()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            privacyPreferences.getScreenPrivacyFlow().collect { enabled ->
                if (enabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            val systemDark = isSystemInDarkTheme()
            val systemColorScheme = if (systemDark) darkColorScheme() else lightColorScheme()
            val colorBackground = if (systemDark) AndroidColor.BLACK else AndroidColor.WHITE
            SideEffect {
                window.setBackgroundDrawable(colorBackground.toDrawable())
            }

            MaterialTheme(colorScheme = systemColorScheme) {
                when (lockState) {
                    LockState.UNLOCKED -> {
                        JuneApp(
                            pendingRoute = pendingRoute,
                            onRouteConsumed = { pendingRoute = null }
                        )
                    }

                    LockState.LOCKED_PIN -> {
                        PinLockScreen(
                            title = "Enter PIN",
                            isError = isPinError,
                            onForgotPin = if (storedSecurityQuestion != null) {
                                { lockState = LockState.LOCKED_SECURITY_QUESTION }
                            } else null,
                            onPinSubmitted = { inputPin ->
                                val inputHash = SecurityUtils.hashPin(inputPin)
                                if (inputHash == storedPinHash) {
                                    onUnlocked()
                                    isPinError = false
                                } else {
                                    isPinError = true
                                }
                            }
                        )
                    }

                    LockState.LOCKED_SECURITY_QUESTION -> {
                        PinRecoveryScreen(
                            question = storedSecurityQuestion ?: "Security Question",
                            storedAnswerHash = storedSecurityAnswerHash ?: "",
                            onBackClick = { lockState = LockState.LOCKED_PIN },
                            onPinResetSuccess = {
                                lifecycleScope.launch {
                                    privacyPreferences.updatePinHash(null)
                                    privacyPreferences.updateSecurityQuestionAndAnswer(null, null)
                                    privacyPreferences.updateAppLock(false)
                                }
                                onUnlocked()
                            }
                        )
                    }

                    else -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_background),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(CircleShape)
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "June Logo",
                                    modifier = Modifier.size(240.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkBiometricAndAuthenticate() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = biometricManager.canAuthenticate(authenticators)

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            authenticateUser()
        } else {
            onUnlocked()
        }
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
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
                        finish()
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Open June")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeActivity?.get() == this) {
            activeActivity = null
        }
    }
}