package com.denser.june

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.domain.preferences.FontPreferences
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.core.domain.model.getAppThemeFlow
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.components.PinLockScreen
import com.denser.june.core.utils.SecurityUtils
import com.denser.june.presentation.JuneApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import com.denser.june.core.R
import androidx.core.graphics.drawable.toDrawable

enum class LockState {
    LOADING,
    LOCKED_BIOMETRIC,
    LOCKED_PIN,
    UNLOCKED
}

class MainActivity : FragmentActivity() {

    private val privacyPreferences: PrivacyPreferences by inject()
    private val themePrefs: ThemePreferences by inject()
    private val fontPrefs: FontPreferences by inject()
    private var lockState by mutableStateOf(LockState.LOADING)

    private var isPinError by mutableStateOf(false)
    private var storedPinHash: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialAppTheme = runBlocking {
            themePrefs.getAppThemeFlow(fontPrefs).first()
        }

        splashScreen.setKeepOnScreenCondition { lockState == LockState.LOADING }

        lifecycleScope.launch {
            val isLockEnabled = privacyPreferences.getAppLockFlow().first()
            val lockType = privacyPreferences.getLockTypeFlow().first()
            storedPinHash = privacyPreferences.getPinHashFlow().first()

            if (!isLockEnabled) {
                lockState = LockState.UNLOCKED
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
                            lockState = LockState.UNLOCKED
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
            window.setBackgroundDrawable(colorBackground.toDrawable())

            MaterialTheme(colorScheme = systemColorScheme) {
                when (lockState) {
                    LockState.UNLOCKED -> {
                        JuneApp(initialAppTheme = initialAppTheme)
                    }

                    LockState.LOCKED_PIN -> {
                        PinLockScreen(
                            title = "Enter PIN",
                            isError = isPinError,
                            onPinSubmitted = { inputPin ->
                                val inputHash = SecurityUtils.hashPin(inputPin)
                                if (inputHash == storedPinHash) {
                                    lockState = LockState.UNLOCKED
                                    isPinError = false
                                } else {
                                    isPinError = true
                                }
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
            lockState = LockState.UNLOCKED
        }
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    lockState = LockState.UNLOCKED
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
}