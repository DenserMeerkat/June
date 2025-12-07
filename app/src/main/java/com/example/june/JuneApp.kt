package com.example.june

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.june.core.presentation.screens.home.HomeScreen
import com.example.june.core.presentation.screens.settings.SettingsGraph
import com.example.june.viewmodels.SettingsVM
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
sealed class Route {
    @Serializable
    data object Home : Route()

    @Serializable
    data object SettingsGraph : Route()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneApp() {
    val settingsVM: SettingsVM = koinViewModel()
    val settingsState by settingsVM.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    _root_ide_package_.com.example.june.core.presentation.theme.JuneTheme(
        theme = settingsState.theme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = Route.Home,
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
                composable<Route.Home> {
                    HomeScreen(
                        onNavigateToSettings = {
                            navController.navigate(Route.SettingsGraph)
                        }
                    )
                }

                composable<Route.SettingsGraph> {
                    SettingsGraph(
                        state = settingsState,
                        onAction = settingsVM::onAction,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}