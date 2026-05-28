package com.denser.june.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.denser.june.presentation.components.JuneMediaLightbox
import com.denser.june.presentation.screens.editor.EditorVM
import com.denser.june.presentation.screens.editor.EditorScreen
import com.denser.june.presentation.screens.editor.screens.ItemGalleryScreen
import com.denser.june.presentation.screens.home.HomeScreen
import com.denser.june.presentation.screens.search.SearchScreen
import com.denser.june.presentation.screens.settings.FontSelectionScreen
import com.denser.june.presentation.screens.settings.SettingsScreen
import com.denser.june.presentation.screens.settings.screens.AboutLibrariesScreen
import com.denser.june.presentation.screens.settings.screens.BackupScreen
import com.denser.june.presentation.screens.settings.screens.LockMethodScreen
import com.denser.june.presentation.screens.settings.screens.PermissionsScreen
import com.denser.june.presentation.screens.settings.screens.PinSetupScreen
import com.denser.june.presentation.screens.settings.screens.reminder.ReminderScreen
import com.denser.june.presentation.screens.settings.screens.sync.SyncScreen
import com.denser.june.presentation.screens.settings.screens.trash.BinScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun JuneNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn()
        },
        exitTransition = {
            fadeOut(targetAlpha = 0.5f)
        },
        popEnterTransition = {
            fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        }
    ) {
        composable<Route.Home> {
            HomeScreen()
        }

        composable<Route.Search>(
            enterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() }
        ) {
            SearchScreen()
        }

        composable<Route.Editor> {
            EditorScreen()
        }

        composable<Route.JournalMedia> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<Route.Editor>()
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
                navController.getBackStackEntry<Route.Editor>()
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
            SettingsScreen()
        }

        composable<Route.Backup> {
            BackupScreen()
        }

        composable<Route.Permissions> {
            PermissionsScreen()
        }

        composable<Route.AboutLibraries> {
            AboutLibrariesScreen()
        }

        composable<Route.LockMethod> {
            LockMethodScreen()
        }

        composable<Route.PinSetup> {
            PinSetupScreen()
        }

        composable<Route.Bin> {
            BinScreen()
        }

        composable<Route.SyncSettings> {
            SyncScreen()
        }

        composable<Route.ReminderSettings> {
            ReminderScreen()
        }

        composable<Route.Fonts> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.Fonts>()
            FontSelectionScreen(type = args.type)
        }
    }
}