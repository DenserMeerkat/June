package com.example.june.core.presentation.screens.home

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.screens.home.components.FloatingBottomBar
import com.example.june.core.presentation.screens.home.journals.JournalsPage
import com.example.june.core.presentation.screens.home.chats.ChatsPage
import com.example.june.core.presentation.screens.settings.components.EditChatSheet
import com.example.june.core.presentation.components.JuneTopAppBar
import com.example.june.viewmodels.HomeChatVM
import org.koin.compose.koinInject

import com.example.june.R
import com.example.june.core.presentation.components.JuneAppBarType

enum class HomeNavItem(
    val route: String,
    val title: String,
    val icon: Int,
    val selectedIcon: Int
) {
    JOURNALS("journals", "Journals", R.drawable.list_alt_24px, R.drawable.list_alt_24px_fill),
    REWIND("rewind", "Rewind", R.drawable.calendar_view_day_24px, R.drawable.calendar_view_day_24px_fill),
    CHATS("chats", "Chats", R.drawable.forum_24px, R.drawable.forum_24px_fill),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    val navigator = koinInject<AppNavigator>()
    val chatViewModel = koinInject<HomeChatVM>()

    val homeNavController = rememberNavController()
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showCreateChatSheet by remember { mutableStateOf(false) }

    fun getRouteIndex(route: String?): Int {
        return when (route) {
            HomeNavItem.JOURNALS.route -> 0
            HomeNavItem.REWIND.route -> 1
            HomeNavItem.CHATS.route -> 2
            else -> 0
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                JuneTopAppBar(
                    type = JuneAppBarType.CenterAligned,
                    title = {
                        Text(
                            text = "June",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        FilledIconButton(
                            onClick = { navigator.navigateTo(Route.Settings) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.settings_24px),
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                MediumFloatingActionButton(
                    onClick = { navigator.navigateTo(Route.Journal(null)) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_2_24px),
                        contentDescription = "Add Journal"
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                JournalsPage()
            }
//            NavHost(
//                navController = homeNavController,
//                startDestination = HomeNavItem.JOURNALS.route,
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(innerPadding),
//
//                enterTransition = {
//                    val fromIndex = getRouteIndex(initialState.destination.route)
//                    val toIndex = getRouteIndex(targetState.destination.route)
//                    if (toIndex > fromIndex) slideIntoContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Left,
//                        tween(300)
//                    )
//                    else slideIntoContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Right,
//                        tween(300)
//                    )
//                },
//                exitTransition = {
//                    val fromIndex = getRouteIndex(initialState.destination.route)
//                    val toIndex = getRouteIndex(targetState.destination.route)
//                    if (toIndex > fromIndex) slideOutOfContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Left,
//                        tween(300)
//                    )
//                    else slideOutOfContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Right,
//                        tween(300)
//                    )
//                },
//                popEnterTransition = {
//                    val fromIndex = getRouteIndex(initialState.destination.route)
//                    val toIndex = getRouteIndex(targetState.destination.route)
//                    if (toIndex > fromIndex) slideIntoContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Left,
//                        tween(300)
//                    )
//                    else slideIntoContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Right,
//                        tween(300)
//                    )
//                },
//                popExitTransition = {
//                    val fromIndex = getRouteIndex(initialState.destination.route)
//                    val toIndex = getRouteIndex(targetState.destination.route)
//                    if (toIndex > fromIndex) slideOutOfContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Left,
//                        tween(300)
//                    )
//                    else slideOutOfContainer(
//                        AnimatedContentTransitionScope.SlideDirection.Right,
//                        tween(300)
//                    )
//                }
//            ) {
//                composable(HomeNavItem.JOURNALS.route) {
//                    JournalsPage()
//                }
//
//                composable(HomeNavItem.REWIND.route) {
//                    RewindContent()
//                }
//
//                composable(HomeNavItem.CHATS.route) {
//                    ChatsPage()
//                }
//            }
        }

//        FloatingBottomBar(
//            currentRoute = currentRoute ?: HomeNavItem.JOURNALS.route,
//            onItemSelected = { route ->
//                homeNavController.navigate(route) {
//                    popUpTo(homeNavController.graph.startDestinationId) { saveState = true }
//                    launchSingleTop = true
//                    restoreState = true
//                }
//            },
//            onFabClick = {
//                when (currentRoute) {
//                    HomeNavItem.JOURNALS.route -> {
//                        navigator.navigateTo(Route.Journal(null))
//                    }
//
//                    HomeNavItem.CHATS.route -> {
//                        showCreateChatSheet = true
//                    }
//
//                    else -> {}
//                }
//            },
//        )
    }

    if (showCreateChatSheet) {
        EditChatSheet(
            onDismiss = { showCreateChatSheet = false },
            onSave = { name, uri ->
                chatViewModel.createChat(name, uri?.toString())
            }
        )
    }
}

@Composable
fun RewindContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.calendar_view_day_24px),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Rewind your memories",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}