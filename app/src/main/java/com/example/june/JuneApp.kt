package com.example.june

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.june.core.navigation.AppNavigatorImpl
import com.example.june.core.navigation.NavigationIntent
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.screens.home.HomeScreen
import com.example.june.core.presentation.screens.note.NoteScreen
import com.example.june.core.presentation.screens.settings.SettingsScreen
import com.example.june.core.presentation.screens.settings.section.AboutLibrariesPage
import com.example.june.core.presentation.theme.JuneTheme
import com.example.june.viewmodels.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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

    JuneTheme(
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
                    HomeScreen()
                }

                composable<Route.Settings> {
                    SettingsScreen(
                        state = settingsState,
                        onAction = settingsVM::onAction
                    )
                }

                composable<Route.Note> {
                    NoteScreen()
                }

                composable<Route.AboutLibraries> {
                    AboutLibrariesPage()
                }
            }
        }
    }
}