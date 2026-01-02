package com.example.june.core.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.screens.home.journals.JournalsPage
import com.example.june.core.presentation.components.JuneTopAppBar
import org.koin.compose.koinInject

import com.example.june.R
import com.example.june.core.presentation.components.JuneAppBarType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    val navigator = koinInject<AppNavigator>()

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
                    navigationIcon = {
                        FilledIconButton(
                            onClick = { navigator.navigateTo(Route.Search) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search_24px),
                                contentDescription = "Search"
                            )
                        }
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
        }
    }
}