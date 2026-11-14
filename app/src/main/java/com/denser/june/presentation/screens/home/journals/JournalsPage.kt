package com.denser.june.presentation.screens.home.journals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.utils.toLocalDate
import com.denser.june.presentation.components.journal.DayJournalGroupData
import com.denser.june.presentation.components.journal.ExportJournalSheet
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.screens.home.components.DayJournalGroup
import com.denser.june.presentation.components.journal.JournalCard
import com.denser.june.presentation.components.journal.JournalOptionsSheet
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.utils.UiUtils
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.journal.HighlightJournalCard
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private data class SearchFilter(
    val prefix: String,
    val label: String,
    val isSelected: Boolean,
    val onToggle: () -> Unit
)

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun JournalsPage(
    isSelected: Boolean = true,
    isSearchActive: Boolean = false,
    viewModel: JournalsVM = koinViewModel()
) {
    val navigator = koinInject<AppNavigator>()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val is24Hour = timeFormat == TimeFormat.TWENTY_FOUR_HOUR

    val feedState by viewModel.feedUiState.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val isDraft by viewModel.isDraft.collectAsStateWithLifecycle()
    val hasMedia by viewModel.hasMedia.collectAsStateWithLifecycle()
    val hasSong by viewModel.hasSong.collectAsStateWithLifecycle()
    val hasLocation by viewModel.hasLocation.collectAsStateWithLifecycle()
    val hasActiveFilters by viewModel.hasActiveFilters.collectAsStateWithLifecycle()

    val feedListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val listState = if (isSearchActive) searchListState else feedListState

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchListState.scrollToItem(0)
        }
    }

    LaunchedEffect(isBookmarked, isDraft, hasMedia, hasSong, hasLocation) {
        if (isSearchActive) {
            searchListState.scrollToItem(0)
        }
    }

    var selectedJournalForOptions by remember { mutableStateOf<Journal?>(null) }

    var journalToExport by remember { mutableStateOf<Journal?>(null) }

    ExportJournalSheet(
        journal = journalToExport,
        onDismiss = { journalToExport = null }
    )

    val currentJournalForOptions = remember(selectedJournalForOptions, feedState.journals) {
        val id = selectedJournalForOptions?.id ?: return@remember null
        feedState.journals.find { it.id == id }
    }

    JournalOptionsSheet(
        journal = currentJournalForOptions,
        is24Hour = is24Hour,
        onDismiss = { selectedJournalForOptions = null },
        onToggleBookmark = {
            currentJournalForOptions?.id?.let { viewModel.toggleBookmark(it) }
        },
        onToggleLock = {
            currentJournalForOptions?.id?.let { viewModel.toggleLock(it) }
        },
        onExportMarkdown = {
            journalToExport = currentJournalForOptions
            selectedJournalForOptions = null
        },
        onDeleteOrRestore = {
            currentJournalForOptions?.let {
                if (it.isDeleted) {
                    viewModel.restoreJournal(it.id)
                } else {
                    viewModel.deleteJournal(it.id)
                }
            }
        }
    )

    val journals = feedState.journals

    val dayGroups = remember(journals) {
        journals
            .groupBy { it.dateTime.toLocalDate() }
            .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val filters = listOf(
                    SearchFilter(
                        prefix = "is:",
                        label = stringResource(R.string.bookmarked),
                        isSelected = isBookmarked,
                        onToggle = viewModel::toggleBookmarkFilter
                    ),
                    SearchFilter(
                        prefix = "is:",
                        label = stringResource(R.string.draft),
                        isSelected = isDraft,
                        onToggle = viewModel::toggleDraftFilter
                    ),
                    SearchFilter(
                        prefix = "has:",
                        label = stringResource(R.string.media),
                        isSelected = hasMedia,
                        onToggle = viewModel::toggleMediaFilter
                    ),
                    SearchFilter(
                        prefix = "has:",
                        label = stringResource(R.string.music),
                        isSelected = hasSong,
                        onToggle = viewModel::toggleSongFilter
                    ),
                    SearchFilter(
                        prefix = "has:",
                        label = stringResource(R.string.location),
                        isSelected = hasLocation,
                        onToggle = viewModel::toggleLocationFilter
                    )
                )

                JuneConnectedButtonGroup(
                    options = filters,
                    selected = { it.isSelected },
                    onSelect = { it.onToggle() },
                    animateWidthOnPress = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween,
                        Alignment.Start
                    ),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) { filter, _ ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Normal,
                                    color = LocalContentColor.current.copy(alpha = 0.6f)
                                )
                            ) {
                                append(filter.prefix)
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(filter.label)
                            }
                        },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (feedState.isLoading) {
                    item {
                        JunePlaceholderPage(
                            modifier = Modifier.fillParentMaxHeight(0.8f),
                            isLoading = true
                        )
                    }
                } else if (journals.isEmpty()) {
                    item {
                        JunePlaceholderPage(
                            modifier = Modifier.fillParentMaxHeight(0.7f),
                            icon = if (hasActiveFilters) R.drawable.search_off_24px else R.drawable.auto_stories_off_24px,
                            title = stringResource(
                                if (hasActiveFilters) R.string.no_matches_found else R.string.no_journals_yet
                            ),
                            subtitle = stringResource(
                                if (hasActiveFilters) R.string.no_matches_found_desc else R.string.no_journals_yet_desc
                            ),
                            action = if (hasActiveFilters) {
                                {
                                    Button(
                                        onClick = { viewModel.resetAllFilters() }
                                    ) {
                                        Text(stringResource(R.string.clear_filters))
                                    }
                                }
                            } else {
                                {
                                    Button(
                                        onClick = { navigator.navigateTo(Route.Editor()) }
                                    ) {
                                        Text(stringResource(R.string.new_journal))
                                    }
                                }
                            }
                        )
                    }
                } else {
                    val isFilteringOrSearching = isSearchActive || hasActiveFilters
                    val recentJournalId = if (isFilteringOrSearching) null else journals.firstOrNull()?.id

                    if (!isFilteringOrSearching && recentJournalId != null) {
                        item(key = "header_recent") {
                            SectionHeader(
                                title = stringResource(R.string.recent),
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    var hasShownMoreHeader = false

                    dayGroups.forEach { dayGroup ->
                        val containsRecent = recentJournalId != null && dayGroup.journals.any { it.id == recentJournalId }
                        val hasOtherJournals = if (containsRecent) dayGroup.journals.size > 1 else true

                        if (!isFilteringOrSearching && !hasShownMoreHeader && !containsRecent && recentJournalId != null) {
                            item(key = "header_more_${dayGroup.date}") {
                                SectionHeader(
                                    title = stringResource(R.string.more_entries),
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .animateItem()
                                )
                            }
                            hasShownMoreHeader = true
                        }

                        if (dayGroup.journals.size > 1) {
                            item(key = "day_group_${dayGroup.date}") {
                                DayJournalGroup(
                                    date = dayGroup.date,
                                    journals = dayGroup.journals,
                                    is24Hour = is24Hour,
                                    recentJournalId = recentJournalId,
                                    onToggleBookmark = { id -> viewModel.toggleBookmark(id) },
                                    onJournalClick = { journal ->
                                        navigator.navigateTo(Route.Editor(journal.id), isSingleTop = true)
                                    },
                                    onLongClick = { journal -> selectedJournalForOptions = journal },
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .animateItem()
                                )
                            }
                        } else {
                            val singleJournal = dayGroup.journals.first()
                            item(key = "single_journal_${singleJournal.id}") {
                                if (singleJournal.id == recentJournalId) {
                                    HighlightJournalCard(
                                        journal = singleJournal,
                                        is24Hour = is24Hour,
                                        onToggleBookmark = { viewModel.toggleBookmark(singleJournal.id) },
                                        onJournalClick = {
                                            navigator.navigateTo(Route.Editor(singleJournal.id), isSingleTop = true)
                                        },
                                        onLongClick = { selectedJournalForOptions = singleJournal },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateItem()
                                    )
                                } else {
                                    JournalCard(
                                        journal = singleJournal,
                                        is24Hour = is24Hour,
                                        showDate = true,
                                        onToggleBookmark = { viewModel.toggleBookmark(singleJournal.id) },
                                        onJournalClick = {
                                            navigator.navigateTo(Route.Editor(singleJournal.id), isSingleTop = true)
                                        },
                                        onLongClick = { selectedJournalForOptions = singleJournal },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier.height(
                            if (isSearchActive) 16.dp else UiUtils.BOTTOM_BAR_PADDING
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.padding(vertical = 4.dp, horizontal = 24.dp)
    )
}

