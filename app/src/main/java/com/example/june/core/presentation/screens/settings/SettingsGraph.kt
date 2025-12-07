package com.example.june.core.presentation.screens.settings

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.june.core.presentation.screens.settings.section.AboutLibrariesPage
import kotlinx.serialization.Serializable

@Serializable
private sealed interface SettingsRoutes {
    @Serializable
    data object SettingRootPage : SettingsRoutes

    @Serializable
    data object BackupPage : SettingsRoutes

    @Serializable
    data object AboutLibrariesPage : SettingsRoutes
}

@Composable
fun SettingsGraph(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SettingsRoutes.SettingRootPage,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        }
    ) {
        composable<SettingsRoutes.SettingRootPage> {
            SettingsScreen(
                state = state,
                onAction = onAction,
                onNavigateBack = onNavigateBack,
                onNavigateToAboutLibraries = { navController.navigate(SettingsRoutes.AboutLibrariesPage) },
            )
        }

        composable<SettingsRoutes.AboutLibrariesPage> {
            AboutLibrariesPage(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}