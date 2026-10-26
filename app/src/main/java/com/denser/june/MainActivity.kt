package com.denser.june

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.SideEffect
import android.graphics.Color as AndroidColor
import androidx.core.graphics.drawable.toDrawable
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.presentation.JuneApp
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.utils.ExternalIntentProcessor
import com.denser.june.presentation.screens.settings.screens.lock.LockSessionVM
import com.denser.june.presentation.screens.settings.screens.lock.LockGate
import com.denser.june.presentation.screens.settings.screens.lock.LockTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    companion object {
        private var activeActivity: WeakReference<MainActivity>? = null
    }

    private val privacyPreferences: PrivacyPreferences by inject()
    private val journalPreferences: JournalPreferences by inject()
    private val externalIntentProcessor: ExternalIntentProcessor by inject()
    private val lockSessionVM: LockSessionVM by inject()
    private var isAppLockConfigured by mutableStateOf<Boolean?>(null)
    private var pendingRoute by mutableStateOf<Route?>(null)
    private var pendingIntentRoute: Route? = null

    private fun onUnlocked() {
        lockSessionVM.unlockApp()
        pendingIntentRoute?.let { route ->
            pendingRoute = route
            pendingIntentRoute = null
        }
    }

    override fun onStop() {
        super.onStop()
        lockSessionVM.clearAll()
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
            if (lockSessionVM.isAppUnlocked.value) {
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

        splashScreen.setKeepOnScreenCondition { isAppLockConfigured == null }
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        lifecycleScope.launch {
            val isLockEnabled = privacyPreferences.getAppLockFlow().first()
            isAppLockConfigured = isLockEnabled

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
                    if (lockSessionVM.isAppUnlocked.value) {
                        pendingRoute = targetRoute
                    } else {
                        pendingIntentRoute = targetRoute
                    }
                }
            }

            if (!isLockEnabled) {
                onUnlocked()
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
                val isUnlocked by lockSessionVM.isAppUnlocked.collectAsStateWithLifecycle()
                val isLockEnabled = isAppLockConfigured ?: false

                if (!isLockEnabled || isUnlocked) {
                    JuneApp(
                        pendingRoute = pendingRoute,
                        onRouteConsumed = { pendingRoute = null }
                    )
                } else {
                    LockGate(
                        target = LockTarget.APP,
                        showBackButton = false,
                        onUnlocked = { onUnlocked() },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeActivity?.get() == this) {
            activeActivity = null
        }
    }
}
