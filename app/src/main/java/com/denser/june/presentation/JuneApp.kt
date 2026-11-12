package com.denser.june.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.denser.june.MainVM
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.presentation.navigation.AppNavigator
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.denser.june.presentation.navigation.JuneNavHost
import com.denser.june.presentation.navigation.NavigationIntent
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.theme.JuneTheme
import com.denser.june.presentation.theme.LocalAppTheme
import com.denser.june.presentation.theme.LocalInternetAllowed
import com.denser.june.presentation.theme.LocalSyncEnabled
import com.denser.june.presentation.utils.StartupManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import com.denser.june.core.domain.billing.BillingHandler
import com.denser.june.presentation.plus.JunePlusPaywallScreen
import com.denser.june.presentation.plus.LocalIsPlus
import com.denser.june.presentation.plus.LocalPlusSheetController
import com.denser.june.presentation.plus.rememberPlusSheetController
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import com.denser.june.presentation.screens.settings.components.sheets.WhatsNewBottomSheet
import com.denser.june.presentation.utils.handleAnnouncementAction

@Composable
fun JuneApp(
    pendingRoute: Route? = null,
    onRouteConsumed: () -> Unit = {}
) {
    val mainVM: MainVM = koinViewModel()
    val appState by mainVM.state.collectAsStateWithLifecycle()

    val navigator = koinInject<AppNavigator>()
    val backStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Route>, String>(
            save = { list -> list.map { Json.encodeToString(Route.serializer(), it) } },
            restore = { savedList ->
                mutableStateListOf<Route>().apply {
                    val restored = savedList.mapNotNull {
                        runCatching { Json.decodeFromString(Route.serializer(), it) }.getOrNull()
                    }
                    addAll(if (restored.isEmpty()) listOf(Route.Home) else restored)
                }
            }
        )
    ) {
        mutableStateListOf<Route>(Route.Home)
    }

    val startupManager = koinInject<StartupManager>()
    val pendingWhatsChanged by startupManager.pendingWhatsChanged.collectAsStateWithLifecycle(initialValue = null)
    val pendingAnnouncement by startupManager.pendingAnnouncement.collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            while (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
            if (backStack.isEmpty()) {
                backStack.add(Route.Home)
            }
            if (route != Route.Home) {
                backStack.add(route)
            }
            onRouteConsumed()
        }
    }

    LaunchedEffect(Unit) {
        startupManager.checkStartupFlows()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.navigationActions.collect { intent ->
                when (intent) {
                    is NavigationIntent.NavigateBack -> {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        } else if (backStack.firstOrNull() != Route.Home) {
                            backStack.clear()
                            backStack.add(Route.Home)
                        }
                    }
                    is NavigationIntent.NavigateTo -> {
                        if (intent.popUpToRoute != null) {
                            val targetClass = intent.popUpToRoute::class
                            val index = backStack.indexOfLast { it::class == targetClass }
                            if (index != -1) {
                                val fromIndex = if (intent.inclusive) index else index + 1
                                while (backStack.size > fromIndex) {
                                    backStack.removeAt(backStack.lastIndex)
                                }
                            }
                        }
                        if (intent.isSingleTop && backStack.lastOrNull() == intent.route) {
                            return@collect
                        }
                        backStack.add(intent.route)
                    }
                }
            }
        }
    }

    val billingHandler = koinInject<BillingHandler>()
    val isPlus by billingHandler.isPlus.collectAsStateWithLifecycle()
    val plusSheetController = rememberPlusSheetController()

    val uiDirection = if (appState.isForceLtrUi) LayoutDirection.Ltr else LocalLayoutDirection.current

    CompositionLocalProvider(
        LocalAppTheme provides appState.appTheme,
        LocalInternetAllowed provides appState.isInternetAllowed,
        LocalSyncEnabled provides appState.isSyncEnabled,
        LocalLayoutDirection provides uiDirection,
        LocalIsPlus provides isPlus,
        LocalPlusSheetController provides plusSheetController
    ) {
        JuneTheme(appTheme = appState.appTheme) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JuneNavHost(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            } else if (backStack.firstOrNull() != Route.Home) {
                                backStack.clear()
                                backStack.add(Route.Home)
                            }
                        }
                    )
                }

                JunePlusPaywallScreen(controller = plusSheetController)
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
                                    uriHandler = uriHandler,
                                    onOpenPlus = { plusSheetController.show() }
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
