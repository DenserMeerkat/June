package com.denser.june.presentation.screens.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.home.components.JournalCard
import com.denser.june.presentation.utils.UiUtils
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen() {
    val viewModel: SearchVM = koinViewModel()
    val navigator = koinInject<AppNavigator>()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val isDraft by viewModel.isDraft.collectAsStateWithLifecycle()
    val hasMedia by viewModel.hasMedia.collectAsStateWithLifecycle()
    val hasSong by viewModel.hasSong.collectAsStateWithLifecycle()
    val hasLocation by viewModel.hasLocation.collectAsStateWithLifecycle()
    val searchState by viewModel.searchResults.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Column {
                JuneTopAppBar(
                    title = {
                        TextField(
                            value = query,
                            onValueChange = viewModel::onQueryChange,
                            placeholder = { Text("Search your journal") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            colors = UiUtils.getTransparentTextFieldColors(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(
                                            painterResource(R.drawable.close_24px),
                                            contentDescription = "Clear"
                                        )
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        FilledIconButton(
                            onClick = { navigator.navigateBack() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back_24px),
                                contentDescription = "Back",
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    SearchFilterChip(
                        selected = isBookmarked,
                        onClick = viewModel::toggleBookmarkFilter,
                        icon = R.drawable.bookmark_added_24px_fill,
                        prefix = "is:",
                        label = "Bookmarked"
                    )
                    SearchFilterChip(
                        selected = isDraft,
                        onClick = viewModel::toggleDraftFilter,
                        icon = R.drawable.edit_24px_fill,
                        prefix = "is:",
                        label = "Draft"
                    )
                    SearchFilterChip(
                        selected = hasMedia,
                        onClick = viewModel::toggleMediaFilter,
                        icon = R.drawable.photo_24px_fill,
                        prefix = "has:",
                        label = "Media"
                    )
                    SearchFilterChip(
                        selected = hasSong,
                        onClick = viewModel::toggleSongFilter,
                        icon = R.drawable.music_note_24px,
                        prefix = "has:",
                        label = "Music"
                    )
                    SearchFilterChip(
                        selected = hasLocation,
                        onClick = viewModel::toggleLocationFilter,
                        icon = R.drawable.location_on_24px_fill,
                        prefix = "has:",
                        label = "Location"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            when {
                searchState.isIdle -> {
                    JunePlaceholderPage(
                        icon = R.drawable.search_24px,
                        title = "Search your journal",
                        subtitle = "Type a keyword or select a filter above to find specific entries."
                    )
                }
                searchState.isLoading -> {
                    JunePlaceholderPage(
                        isLoading = true
                    )
                }
                searchState.journals.isEmpty() -> {
                    JunePlaceholderPage(
                        icon = R.drawable.search_off_24px,
                        title = "No matches found",
                        subtitle = "We couldn't find any journals matching your current search or filters."
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = searchState.journals, key = { it.id }) { journal ->
                            JournalCard(
                                journal = journal,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    icon: Int,
    prefix: String,
    label: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Normal,
                            color = LocalContentColor.current.copy(alpha = 0.6f)
                        )
                    ) {
                        append(prefix)
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(label)
                    }
                }
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    painterResource(icon),
                    null,
                    Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
}