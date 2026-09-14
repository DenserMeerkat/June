package com.denser.june.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.denser.june.MainVM
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.JuneNavHost
import com.denser.june.presentation.navigation.NavigationIntent
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.theme.JuneTheme
import com.denser.june.presentation.theme.LocalAppTheme
import com.denser.june.presentation.theme.LocalInternetAllowed
import com.denser.june.presentation.theme.LocalSyncEnabled
import com.denser.june.presentation.utils.StartupManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import com.denser.june.presentation.screens.settings.components.WhatsNewBottomSheet
import com.denser.june.presentation.utils.handleAnnouncementAction

@Composable
fun JuneApp(
    pendingRoute: Route? = null,
    onRouteConsumed: () -> Unit = {}
) {
    val mainVM: MainVM = koinViewModel()
    val appState by mainVM.state.collectAsStateWithLifecycle()

    val navigator = koinInject<AppNavigator>()
    val navController = rememberNavController()

    val startupManager = koinInject<StartupManager>()
    val pendingWhatsChanged by startupManager.pendingWhatsChanged.collectAsStateWithLifecycle(initialValue = null)
    val pendingAnnouncement by startupManager.pendingAnnouncement.collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navController.navigate(route) {
                popUpTo(Route.Home) { inclusive = false }
                launchSingleTop = true
            }
            onRouteConsumed()
        }
    }

    LaunchedEffect(Unit) {
        startupManager.checkStartupFlows()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            navigator.navigationActions.collect { intent ->
                when (intent) {
                    is NavigationIntent.NavigateBack -> {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Route.Home) {
                                popUpTo(Route.Home) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                    is NavigationIntent.NavigateTo -> {
                        navController.navigate(intent.route) {
                            intent.popUpToRoute?.let { popUpRoute ->
                                popUpTo(popUpRoute) { inclusive = intent.inclusive }
                            }
                            launchSingleTop = intent.isSingleTop
                        }
                    }
                }
            }
        }
    }

    val uiDirection = if (appState.isForceLtrUi) LayoutDirection.Ltr else LocalLayoutDirection.current

    CompositionLocalProvider(
        LocalAppTheme provides appState.appTheme,
        LocalInternetAllowed provides appState.isInternetAllowed,
        LocalSyncEnabled provides appState.isSyncEnabled,
        LocalLayoutDirection provides uiDirection
    ) {
        JuneTheme(appTheme = appState.appTheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                JuneNavHost(
                    navController = navController,
                    startDestination = Route.Home
                )
            }

            val uriHandler = LocalUriHandler.current

            if (pendingWhatsChanged != null || pendingAnnouncement != null) {
                WhatsNewBottomSheet(
                    versionEntry = pendingWhatsChanged,
                    announcement = pendingAnnouncement,
                    onDismissRequest = {
                        coroutineScope.launch {
                            pendingWhatsChanged?.let { startupManager.dismissWhatsChanged(it.version) }
                            pendingAnnouncement?.let { startupManager.dismissAnnouncement(it.id) }
                        }
                    },
                    onAnnouncementAction = { announcement ->
                        coroutineScope.launch {
                            announcement.action?.let { action ->
                                handleAnnouncementAction(
                                    action = action,
                                    navigator = navigator,
                                    uriHandler = uriHandler
                                )
                            }
                            startupManager.dismissAnnouncement(announcement.id)
                            pendingWhatsChanged?.let { startupManager.dismissWhatsChanged(it.version) }
                        }
                    }
                )
            }
        }
    }
}