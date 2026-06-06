package com.denser.june.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.denser.june.MainVM
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.JuneNavHost
import com.denser.june.presentation.navigation.NavigationIntent
import com.denser.june.presentation.theme.JuneTheme
import com.denser.june.presentation.theme.LocalAppTheme
import com.denser.june.presentation.theme.LocalInternetAllowed
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JuneApp(initialAppTheme: AppTheme) {
    val mainVM: MainVM = koinViewModel(parameters = { parametersOf(initialAppTheme) })
    val appState by mainVM.state.collectAsStateWithLifecycle()

    val navigator = koinInject<AppNavigator>()
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

    CompositionLocalProvider(
        LocalAppTheme provides appState.appTheme,
        LocalInternetAllowed provides appState.isInternetAllowed
    ) {
        JuneTheme(appTheme = appState.appTheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                JuneNavHost(
                    navController = navController,
                )
            }
        }
    }
}