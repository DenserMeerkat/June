package com.example.june.core.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface NavigationIntent {
    data class NavigateTo(
        val route: Route,
        val popUpToRoute: Route? = null,
        val inclusive: Boolean = false,
        val isSingleTop: Boolean = false
    ) : NavigationIntent
    data object NavigateBack : NavigationIntent
}

interface AppNavigator {
    fun navigateTo(
        route: Route,
        popUpToRoute: Route? = null,
        inclusive: Boolean = false,
        isSingleTop: Boolean = false
    )
    fun navigateBack()
}

class AppNavigatorImpl : AppNavigator {
    private val _navigationActions = Channel<NavigationIntent>()
    val navigationActions = _navigationActions.receiveAsFlow()

    override fun navigateTo(
        route: Route,
        popUpToRoute: Route?,
        inclusive: Boolean,
        isSingleTop: Boolean
    ) {
        _navigationActions.trySend(
            NavigationIntent.NavigateTo(
                route,
                popUpToRoute,
                inclusive,
                isSingleTop
            )
        )
    }

    override fun navigateBack() {
        _navigationActions.trySend(NavigationIntent.NavigateBack)
    }
}