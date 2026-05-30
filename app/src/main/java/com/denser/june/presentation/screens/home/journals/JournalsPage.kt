package com.denser.june.presentation.screens.home.journals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.screens.home.components.DeleteConfirmationSheet
import com.denser.june.presentation.screens.home.components.JournalCard
import com.denser.june.presentation.screens.home.components.JournalOptionsSheet
import com.denser.june.presentation.screens.home.components.RecentJournalCard
import com.denser.june.presentation.utils.UiUtils
import org.koin.compose.viewmodel.koinViewModel

enum class JournalListTab(
    val title: String,
    val iconRes: Int,
    val filledIconRes: Int,
    val widthWeight: Float
) {
    Journals("Journals", R.drawable.list_alt_24px, R.drawable.list_alt_24px_fill, 1f),
    Bookmarks("Bookmarks", R.drawable.bookmark_24px, R.drawable.bookmark_24px_fill, 1.1f),
    Drafts("Drafts", R.drawable.edit_note_24px, R.drawable.edit_note_24px_fill, 0.9f)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun JournalsPage(
    isSelected: Boolean = true
) {
    val viewModel: JournalsVM = koinViewModel()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val is24Hour = timeFormat == TimeFormat.TWENTY_FOUR_HOUR
    val nonDrafts by viewModel.nonDraftJournals.collectAsStateWithLifecycle()
    val bookmarkedJournals by viewModel.bookmarkedJournals.collectAsStateWithLifecycle()
    val draftJournals by viewModel.draftJournals.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()


    var selectedJournalForOptions by remember { mutableStateOf<Journal?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val deleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = isSelected && selectedTab != JournalListTab.Journals) {
        viewModel.onTabSelected(JournalListTab.Journals)
    }

    fun dismissSheet(action: () -> Unit) {
        action()
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            selectedJournalForOptions = null
        }
    }

    val currentJournalForOptions = remember(selectedJournalForOptions, nonDrafts, bookmarkedJournals, draftJournals) {
        val id = selectedJournalForOptions?.id ?: return@remember null
        (nonDrafts ?: emptyList()).find { it.id == id }
            ?: (bookmarkedJournals ?: emptyList()).find { it.id == id }
            ?: (draftJournals ?: emptyList()).find { it.id == id }
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
                    if (selectedTab == JournalListTab.Bookmarks) {
                        dismissSheet { viewModel.toggleBookmark(currentJournalForOptions.id) }
                    } else {
                        viewModel.toggleBookmark(currentJournalForOptions.id)
                    }
                },
                onDeleteOrRestore = {
                    if (currentJournalForOptions.isDeleted) {
                        dismissSheet { viewModel.restoreJournal(currentJournalForOptions.id) }
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

    val recentJournal = remember(nonDrafts) { nonDrafts?.firstOrNull() }
    val moreJournals = remember(nonDrafts) { nonDrafts?.drop(1) ?: emptyList() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val tabs = JournalListTab.entries
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == tab
                    val shape = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }

                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { viewModel.onTabSelected(tab) },
                        shapes = shape,
                        modifier = Modifier.weight(tab.widthWeight),
                        contentPadding = PaddingValues(6.dp, 4.dp),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.onSecondary,
                            checkedContentColor = MaterialTheme.colorScheme.secondary,
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = if (isSelected) tab.filledIconRes else tab.iconRes),
                                contentDescription = tab.title,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    JournalListTab.Journals -> {
                        if (nonDrafts == null) {
                            item {
                                JunePlaceholderPage(
                                    modifier = Modifier.fillParentMaxHeight(0.8f),
                                    isLoading = true
                                )
                            }
                        } else if (nonDrafts?.isEmpty() == true) {
                            item {
                                JunePlaceholderPage(
                                    modifier = Modifier.fillParentMaxHeight(0.8f),
                                    icon = R.drawable.auto_stories_off_24px,
                                    title = "No journals yet",
                                    subtitle = "Start capturing your journey today. Create your first entry to start your collection."
                                )
                            }
                        } else {
                            if (recentJournal != null) {
                                item(key = "header_recent") {
                                    SectionHeader(
                                        title = "Recent",
                                        modifier = Modifier.animateItem()
                                    )
                                }
                                item(key = "recent_${recentJournal.id}") {
                                    RecentJournalCard(
                                        journal = recentJournal,
                                        modifier = Modifier.animateItem(),
                                        onLongClick = { selectedJournalForOptions = recentJournal }
                                    )
                                }
                            }
                            if (moreJournals.isNotEmpty()) {
                                item(key = "header_more") {
                                    SectionHeader(
                                        title = "More entries",
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .animateItem()
                                    )
                                }
                                items(moreJournals, key = { "more_${it.id}" }) { journal ->
                                    JournalCard(
                                        journal = journal,
                                        modifier = Modifier.animateItem(),
                                        onLongClick = { selectedJournalForOptions = journal }
                                    )
                                }
                            }
                        }
                    }
                    JournalListTab.Bookmarks -> {
                        if (bookmarkedJournals == null) {
                            item {
                                JunePlaceholderPage(
                                    modifier = Modifier.fillParentMaxHeight(0.8f),
                                    isLoading = true
                                )
                            }
                        } else if (bookmarkedJournals?.isEmpty() == true) {
                            item {
                                JunePlaceholderPage(
                                    modifier = Modifier.fillParentMaxHeight(0.8f),
                                    icon = R.drawable.bookmarks_24px,
                                    title = "No bookmarks",
                                    subtitle = "Keep favorite moments in reach. Any entry you mark will appear here to revisit."
                                )
                            }
                        } else {
                            items(bookmarkedJournals!!, key = { "bm_${it.id}" }) { journal ->
                                JournalCard(
                                    journal = journal,
                                    modifier = Modifier.animateItem(),
                                    onLongClick = { selectedJournalForOptions = journal }
                                )
                            }
                        }
                    }
                    JournalListTab.Drafts -> {
                        if (draftJournals == null) {
                            item {
                                JunePlaceholderPage(
                                    modifier = Modifier.fillParentMaxHeight(0.8f),
                                    isLoading = true
                                )
                            }
                        } else if (draftJournals?.isEmpty() == true) {
                            item {
                                JunePlaceholderPage(
                                    modifier = Modifier.fillParentMaxHeight(0.8f),
                                    icon = R.drawable.edit_note_24px,
                                    title = "No drafts",
                                    subtitle = "Your unfinished thoughts stay here. Any entry you start will be saved to finish later."
                                )
                            }
                        } else {
                            items(draftJournals!!, key = { "draft_${it.id}" }) { journal ->
                                JournalCard(
                                    journal = journal,
                                    modifier = Modifier.animateItem(),
                                    onLongClick = { selectedJournalForOptions = journal }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(UiUtils.BOTTOM_BAR_PADDING)) }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String, modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.padding(vertical = 4.dp, horizontal = 16.dp)
    )
}