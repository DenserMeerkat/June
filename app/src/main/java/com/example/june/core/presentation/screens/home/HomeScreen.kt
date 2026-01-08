package com.example.june.core.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.june.R
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.components.JuneAppBarType
import com.example.june.core.presentation.components.JuneTopAppBar
import com.example.june.core.presentation.screens.home.components.HomeBottomBar
import com.example.june.core.presentation.screens.home.journals.JournalsPage
import com.example.june.core.presentation.screens.home.timeline.TimelinePage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class HomeTab(val label: String, val iconRes: Int, val enabled: Boolean = true) {
    Journals("Journals", R.drawable.list_alt_24px),
    Timeline("Timeline", R.drawable.event_note_24px),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    val navigator = koinInject<AppNavigator>()
    val pagerState = rememberPagerState(pageCount = { HomeTab.entries.size })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage == 1) {
        scope.launch { pagerState.animateScrollToPage(0) }
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
            }
        ) { innerPadding ->
            HorizontalPager(
                userScrollEnabled = false,
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (HomeTab.entries[page]) {
                    HomeTab.Journals -> JournalsPage()
                    HomeTab.Timeline -> TimelinePage()
                }
            }
        }
        HomeBottomBar(
            pagerState = pagerState,
            onFabClick = { navigator.navigateTo(Route.Journal(null)) }
        )
    }
}
