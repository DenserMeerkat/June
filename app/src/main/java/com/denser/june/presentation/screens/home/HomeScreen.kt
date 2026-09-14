package com.denser.june.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

import androidx.compose.animation.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.denser.june.presentation.screens.home.journals.JournalsVM
import com.denser.june.presentation.utils.UiUtils

enum class HomeTab(@get:StringRes val labelRes: Int, val iconRes: Int, val filledIconRes: Int) {
    Journals(R.string.journals, R.drawable.home_24px, R.drawable.home_24px_fill),
    Tags(R.string.tags, R.drawable.view_cozy_24px, R.drawable.view_cozy_24px_fill),
    Timeline(R.string.timeline, R.drawable.event_note_24px, R.drawable.event_note_24px_fill),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    val navigator = koinInject<AppNavigator>()
    val activity = LocalActivity.current as? androidx.activity.ComponentActivity
    val mainVM: MainVM = if (activity != null) {
        koinViewModel(viewModelStoreOwner = activity)
    } else {
        koinViewModel()
    }
    val appState by mainVM.state.collectAsStateWithLifecycle()
    val journalPrefs = koinInject<JournalPreferences>()
    val isAutoTimeEnabled by journalPrefs.isAutoTimeEnabled().collectAsStateWithLifecycle(initialValue = false)
    
    val pagerState = rememberPagerState(pageCount = { HomeTab.entries.size })
    val scope = rememberCoroutineScope()

    val tagsVM: TagsVM = koinViewModel()
    val activeTag by tagsVM.selectedPrimaryTag.collectAsStateWithLifecycle()

    val journalsVM: JournalsVM = koinViewModel()
    val searchQuery by journalsVM.searchQuery.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        journalsVM.clearSearch()
    }

    BackHandler(enabled = !isSearchActive && pagerState.currentPage != 0) {
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
                    type = if (isSearchActive) JuneAppBarType.Small else JuneAppBarType.CenterAligned,
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = journalsVM::onQueryChange,
                                placeholder = { Text(stringResource(R.string.search_your_journal)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                colors = UiUtils.getTransparentTextFieldColors(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { journalsVM.clearSearch() }) {
                                            Icon(
                                                painterResource(R.drawable.close_24px),
                                                contentDescription = stringResource(R.string.clear)
                                            )
                                        }
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    navigationIcon = {
                        FilledIconButton(
                            onClick = {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    journalsVM.clearSearch()
                                } else {
                                    scope.launch {
                                        if (pagerState.currentPage != 0) {
                                            pagerState.animateScrollToPage(0)
                                        }
                                        isSearchActive = true
                                    }
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isSearchActive) R.drawable.arrow_back_24px else R.drawable.search_24px
                                ),
                                contentDescription = stringResource(
                                    if (isSearchActive) R.string.back else R.string.search
                                )
                            )
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            if (appState.isSyncEnabled && appState.isInternetAllowed) {
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
                                    contentDescription = stringResource(R.string.settings)
                                )
                            }
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
                    .padding(top = innerPadding.calculateTopPadding())
            ) { page ->
                when (HomeTab.entries[page]) {
                    HomeTab.Journals -> JournalsPage(
                        isSelected = pagerState.currentPage == 0,
                        isSearchActive = isSearchActive,
                        viewModel = journalsVM
                    )
                    HomeTab.Tags -> TagsPage()
                    HomeTab.Timeline -> TimelinePage()
                }
            }
        }
        AnimatedVisibility(
            visible = !isSearchActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
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
    navigator.navigateTo(route, isSingleTop = true)
}