package com.denser.june.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.home.components.HomeBottomBar
import com.denser.june.presentation.screens.home.journals.JournalsPage
import com.denser.june.presentation.screens.home.timeline.TimelinePage
import com.denser.june.presentation.screens.home.tags.TagsPage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

import com.denser.june.core.R
import com.denser.june.presentation.screens.home.tags.TagsVM
import com.denser.june.presentation.components.SyncIndicator
import com.denser.june.MainVM
import org.koin.compose.viewmodel.koinViewModel

enum class HomeTab(val label: String, val iconRes: Int, val filledIconRes: Int) {
    Journals("Journals", R.drawable.home_24px, R.drawable.home_24px_fill),
    Tags("Tags", R.drawable.view_cozy_24px, R.drawable.view_cozy_24px_fill),
    Timeline("Timeline", R.drawable.event_note_24px, R.drawable.event_note_24px_fill),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    val navigator = koinInject<AppNavigator>()
    val mainVM: MainVM = koinViewModel()
    val appState by mainVM.state.collectAsStateWithLifecycle()
    val journalPrefs = koinInject<JournalPreferences>()
    val isAutoTimeEnabled by journalPrefs.isAutoTimeEnabled().collectAsStateWithLifecycle(initialValue = false)
    
    val pagerState = rememberPagerState(pageCount = { HomeTab.entries.size })
    val scope = rememberCoroutineScope()

    val tagsVM: TagsVM = koinViewModel()
    val activeTag by tagsVM.selectedPrimaryTag.collectAsStateWithLifecycle()

    BackHandler(enabled = pagerState.currentPage != 0) {
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
                        if (appState.isSyncEnabled) {
                            SyncIndicator(
                                status = appState.syncStatus,
                                onClick = { navigator.navigateTo(Route.SyncSettings) }
                            )
                        }
                        Spacer(Modifier.width(4.dp))
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
                    HomeTab.Journals -> JournalsPage(isSelected = pagerState.currentPage == 0)
                    HomeTab.Tags -> TagsPage()
                    HomeTab.Timeline -> TimelinePage()
                }
            }
        }
        HomeBottomBar(
            pagerState = pagerState,
            onFabClick = {
                val currentTab = HomeTab.entries[pagerState.currentPage]
                handleFabClick(
                    currentTab = currentTab,
                    activeTag = activeTag,
                    isAutoTimeEnabled = isAutoTimeEnabled,
                    navigator = navigator
                )
            }
        )
    }
}

private fun handleFabClick(
    currentTab: HomeTab,
    activeTag: String?,
    isAutoTimeEnabled: Boolean,
    navigator: AppNavigator
) {
    val initialDate = if (isAutoTimeEnabled) System.currentTimeMillis() else null
    val route = if (currentTab == HomeTab.Tags && activeTag != null) {
        Route.Editor(initialDate = initialDate, initialTags = listOf(activeTag))
    } else {
        Route.Editor(initialDate = initialDate)
    }
    navigator.navigateTo(route)
}