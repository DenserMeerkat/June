package com.denser.june.presentation.screens.home.journals

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
import com.denser.june.presentation.components.DayJournalGroupData
import com.denser.june.presentation.components.ExportJournalBottomSheet
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.screens.home.components.DeleteConfirmationSheet
import com.denser.june.presentation.screens.home.components.DayJournalGroup
import com.denser.june.presentation.screens.home.components.JournalCard
import com.denser.june.presentation.screens.home.components.JournalOptionsSheet
import com.denser.june.presentation.components.SearchFilterChip
import com.denser.june.presentation.utils.UiUtils
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.home.components.RecentJournalCard
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class,
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

    val listState = rememberLazyListState()

    var selectedJournalForOptions by remember { mutableStateOf<Journal?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val deleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var journalToExport by remember { mutableStateOf<Journal?>(null) }

    ExportJournalBottomSheet(
        journal = journalToExport,
        onDismiss = { journalToExport = null }
    )

    val currentJournalForOptions = remember(selectedJournalForOptions, feedState.journals) {
        val id = selectedJournalForOptions?.id ?: return@remember null
        feedState.journals.find { it.id == id }
    }

    if (currentJournalForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedJournalForOptions = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            JournalOptionsSheet(
                journal = currentJournalForOptions,
                is24Hour = is24Hour,
                onToggleBookmark = {
                    viewModel.toggleBookmark(currentJournalForOptions.id)
                },
                onExportMarkdown = {
                    journalToExport = currentJournalForOptions
                    selectedJournalForOptions = null
                },
                onDeleteOrRestore = {
                    if (currentJournalForOptions.isDeleted) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            selectedJournalForOptions = null
                            viewModel.restoreJournal(currentJournalForOptions.id)
                        }
                    } else {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showDeleteConfirmation = true
                        }
                    }
                }
            )
        }
    }

    if (showDeleteConfirmation && selectedJournalForOptions != null) {
        DeleteConfirmationSheet(
            sheetState = deleteSheetState,
            onDismissRequest = {
                showDeleteConfirmation = false
                selectedJournalForOptions = null
            },
            onConfirm = {
                val id = selectedJournalForOptions?.id
                scope.launch { deleteSheetState.hide() }.invokeOnCompletion {
                    showDeleteConfirmation = false
                    if (id != null) viewModel.deleteJournal(id)
                    selectedJournalForOptions = null
                }
            }
        )
    }

    val journals = feedState.journals

    val dayGroups = remember(journals) {
        journals
            .groupBy { it.dateTime.toLocalDate() }
            .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchFilterChip(
                    selected = isBookmarked,
                    onClick = viewModel::toggleBookmarkFilter,
                    prefix = "is:",
                    label = stringResource(R.string.bookmarked),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                )
                SearchFilterChip(
                    selected = isDraft,
                    onClick = viewModel::toggleDraftFilter,
                    prefix = "is:",
                    label = stringResource(R.string.draft),
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                )
                SearchFilterChip(
                    selected = hasMedia,
                    onClick = viewModel::toggleMediaFilter,
                    prefix = "has:",
                    label = stringResource(R.string.media),
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                )
                SearchFilterChip(
                    selected = hasSong,
                    onClick = viewModel::toggleSongFilter,
                    prefix = "has:",
                    label = stringResource(R.string.music),
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                )
                SearchFilterChip(
                    selected = hasLocation,
                    onClick = viewModel::toggleLocationFilter,
                    prefix = "has:",
                    label = stringResource(R.string.location),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                )
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
                            )
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
                                    RecentJournalCard(
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

