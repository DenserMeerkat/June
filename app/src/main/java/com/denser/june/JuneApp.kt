package com.denser.june

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.denser.june.core.domain.data_classes.Theme
import com.denser.june.core.navigation.AppNavigatorImpl
import com.denser.june.core.navigation.NavigationIntent
import com.denser.june.core.navigation.Route
import com.denser.june.core.presentation.components.JuneMediaLightbox
import com.denser.june.core.presentation.screens.home.HomeScreen
import com.denser.june.core.presentation.screens.journal.JournalScreen
import com.denser.june.core.presentation.screens.journal.section.ItemGalleryScreen
import com.denser.june.core.presentation.screens.search.SearchScreen
import com.denser.june.core.presentation.screens.settings.SettingsScreen
import com.denser.june.core.presentation.screens.settings.screens.AboutLibrariesScreen
import com.denser.june.core.presentation.screens.settings.screens.BackupScreen
import com.denser.june.core.presentation.screens.settings.screens.LockMethodScreen
import com.denser.june.core.presentation.screens.settings.screens.PermissionsScreen
import com.denser.june.core.presentation.screens.settings.screens.PinSetupScreen
import com.denser.june.core.presentation.theme.JuneTheme
import com.denser.june.viewmodels.EditorVM
import com.denser.june.viewmodels.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

val LocalAppTheme = staticCompositionLocalOf<Theme> {
    error("No AppTheme provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneApp() {
    val settingsVM: SettingsVM = koinViewModel()
    val settingsState by settingsVM.state.collectAsStateWithLifecycle()

    val navigator = koinInject<AppNavigatorImpl>()
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        navigator.navigationActions.collect { intent ->
            when (intent) {
                is NavigationIntent.NavigateBack -> {
                    navController.navigateUp()
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

    CompositionLocalProvider(LocalAppTheme provides settingsState.theme) {
        JuneTheme(
            theme = settingsState.theme
        ) {
            Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Route.Home,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { fadeOut(targetAlpha = 0.5f) },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    composable<Route.Home> {
                        HomeScreen()
                    }

                    composable<Route.Search>(
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        }
                    ) {
                        SearchScreen()
                    }

                    composable<Route.Journal> {
                        JournalScreen()
                    }

                    composable<Route.JournalMedia> { backStackEntry ->
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry<Route.Journal>()
                        }
                        val viewModel: EditorVM = koinViewModel(viewModelStoreOwner = parentEntry)

                        ItemGalleryScreen(viewModel = viewModel)
                    }

                    composable<Route.MediaViewerRoute>(
                        enterTransition = { fadeIn() },
                        popExitTransition = { fadeOut() }
                    ) { backStackEntry ->
                        val args = backStackEntry.toRoute<Route.MediaViewerRoute>()
                        JuneMediaLightbox(
                            mediaPaths = args.mediaPaths,
                            initialIndex = args.initialIndex,
                        )
                    }

                    composable<Route.JournalMediaDetail>(
                        enterTransition = { fadeIn() },
                        popExitTransition = { fadeOut() }
                    ) { backStackEntry ->
                        val args = backStackEntry.toRoute<Route.JournalMediaDetail>()
                        val parentEntry = remember(backStackEntry) {
                            navController.getBackStackEntry<Route.Journal>()
                        }
                        val viewModel: EditorVM = koinViewModel(viewModelStoreOwner = parentEntry)
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val editorImages = remember(state.images) { state.images.reversed() }

                        JuneMediaLightbox(
                            mediaPaths = editorImages,
                            initialIndex = args.initialIndex,
                        )
                    }

                    composable<Route.Settings> {
                        SettingsScreen(
                            state = settingsState,
                            onAction = settingsVM::onAction
                        )
                    }

                    composable<Route.Backup> {
                        BackupScreen(
                            state = settingsState,
                            onAction = settingsVM::onAction
                        )
                    }

                    composable<Route.Permissions> {
                        PermissionsScreen()
                    }

                    composable<Route.AboutLibraries> {
                        AboutLibrariesScreen()
                    }

                    composable<Route.LockMethod> {
                        LockMethodScreen(
                            state = settingsState,
                            onAction = settingsVM::onAction
                        )
                    }

                    composable<Route.PinSetup> {
                        PinSetupScreen(
                            onAction = settingsVM::onAction
                        )
                    }
                }
            }
        }
    }
}