package com.denser.june.presentation.screens.editor.components.emoji

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import com.denser.june.core.domain.preferences.EmojiPreferences
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val SearchBarSquircle = RoundedCornerShape(14.dp)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalEmojiPickerSheet(
    initialEmoji: String? = null,
    onEmojiSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val emojiPreferences = koinInject<EmojiPreferences>()

    var stagedEmoji by rememberSaveable(initialEmoji) { mutableStateOf(initialEmoji) }

    val onTileClick: (String) -> Unit = remember {
        { clickedEmoji ->
            stagedEmoji = if (clickedEmoji == stagedEmoji) null else clickedEmoji
        }
    }

    var emojiAssets by remember { mutableStateOf(getCachedEmojiAssets()) }

    LaunchedEffect(Unit) {
        if (emojiAssets == null) {
            emojiAssets = loadEmojiAssets(context.resources)
        }
    }

    val recentEmojis = remember {
        mutableStateListOf<String>().apply {
            addAll(emojiPreferences.getRecentEmojis())
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            debouncedQuery = ""
        } else {
            delay(250L)
            debouncedQuery = trimmed
        }
    }

    val recentDisplayEntries = remember(recentEmojis, emojiAssets) {
        val assets = emojiAssets ?: return@remember emptyList()
        recentEmojis.map { value ->
            val found = assets.emojisByValue[value]
            if (found != null) {
                val groupKey = assets.skinToneIndex.keyByEmoji[found.emoji]
                val skinToneGroup = groupKey?.let { assets.skinToneIndex.groups[it] }
                EmojiDisplayEntry(
                    emoji = skinToneGroup?.base ?: found,
                    variants = skinToneGroup?.variants ?: listOf(found),
                    displayEmoji = value,
                )
            } else {
                val dummy = BundledEmoji(
                    key = value,
                    emoji = value,
                    name = value,
                    slug = value,
                    category = "recent",
                )
                EmojiDisplayEntry(
                    emoji = dummy,
                    variants = listOf(dummy),
                    displayEmoji = value,
                )
            }
        }
    }

    val visibleCategories = remember(recentDisplayEntries.isNotEmpty()) {
        if (recentDisplayEntries.isNotEmpty()) emojiCategories
        else emojiCategories.filter { it != EmojiCategory.RECENT }
    }

    var selectedCategoryKey by rememberSaveable { mutableStateOf(EmojiCategory.SMILEYS_AND_EMOTIONS.key) }
    val categoryRowState = rememberLazyListState()

    val currentEntries = remember(selectedCategoryKey, emojiAssets, recentDisplayEntries) {
        if (selectedCategoryKey == EmojiCategory.RECENT.key) {
            recentDisplayEntries
        } else {
            emojiAssets?.entriesByCategory?.get(selectedCategoryKey)
                ?: emojiAssets?.entriesByCategory?.get(EmojiCategory.SMILEYS_AND_EMOTIONS.key)
                ?: emptyList()
        }
    }

    val searchResults = remember(emojiAssets, debouncedQuery) {
        val assets = emojiAssets ?: return@remember emptyList()
        if (debouncedQuery.isEmpty()) {
            emptyList()
        } else {
            val ranked = searchAndRankEmojis(
                emojis = assets.emojis,
                selectedCategoryKey = "",
                rawQuery = debouncedQuery,
            )
            collapseEmojiSkinToneVariants(ranked, assets.skinToneIndex)
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = { if (stagedEmoji != null) stagedEmoji = null },
                        shape = SearchBarSquircle,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (stagedEmoji != null) {
                                Text(
                                    text = stagedEmoji.orEmpty(),
                                    fontSize = 22.sp,
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.mood_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = SearchBarSquircle,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search_emojis),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close_24px),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = debouncedQuery.isEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    CategoryIconBar(
                        categories = visibleCategories,
                        selectedCategoryKey = selectedCategoryKey,
                        onCategoryClick = { category ->
                            selectedCategoryKey = category.key
                        },
                        rowState = categoryRowState,
                    )
                }

                if (emojiAssets == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ContainedLoadingIndicator()
                    }
                } else if (debouncedQuery.isNotEmpty()) {
                    EmojiGridView(
                        entries = searchResults,
                        stagedEmoji = stagedEmoji,
                        isRecent = false,
                        onTileClick = onTileClick,
                        onRemoveRecent = null,
                        keyPrefix = "search",
                        modifier = Modifier.weight(1f),
                        emptyContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.search_24px),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.no_emojis_found),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                    )
                } else {
                    key(selectedCategoryKey) {
                        val isRecent = selectedCategoryKey == EmojiCategory.RECENT.key
                        EmojiGridView(
                            entries = currentEntries,
                            stagedEmoji = stagedEmoji,
                            isRecent = isRecent,
                            onTileClick = onTileClick,
                            onRemoveRecent = if (isRecent) {
                                { emoji ->
                                    emojiPreferences.removeRecentEmoji(emoji)
                                    recentEmojis.remove(emoji)
                                    if (stagedEmoji == emoji) stagedEmoji = null
                                }
                            } else null,
                            keyPrefix = selectedCategoryKey,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            JuneFloatingActionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                JuneFloatingAction(
                    onClick = onDismiss,
                    label = stringResource(R.string.cancel),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                JuneFloatingAction(
                    onClick = {
                        onEmojiSelected(stagedEmoji)
                        stagedEmoji?.let { emojiPreferences.addRecentEmoji(it) }
                        onDismiss()
                    },
                    label = stringResource(R.string.done),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.check_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryIconBar(
    categories: List<EmojiCategory>,
    selectedCategoryKey: String,
    onCategoryClick: (EmojiCategory) -> Unit,
    rowState: LazyListState,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(selectedCategoryKey) {
        val index = categories.indexOfFirst { it.key == selectedCategoryKey }
        if (index >= 0) {
            val isVisible = rowState.layoutInfo.visibleItemsInfo.any { it.index == index }
            if (!isVisible) {
                rowState.animateScrollToItem(index)
            }
        }
    }

    LazyRow(
        state = rowState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(categories, key = { _, category -> category.key }) { index, category ->
            val isSelected = category.key == selectedCategoryKey
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                categories.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onCategoryClick(category) },
                shapes = shapes,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .height(36.dp)
                    .width(44.dp),
            ) {
                Icon(
                    painter = painterResource(if (isSelected) category.iconFilledRes else category.iconRes),
                    contentDescription = stringResource(category.labelRes),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EmojiGridView(
    entries: List<EmojiDisplayEntry>,
    stagedEmoji: String?,
    isRecent: Boolean,
    onTileClick: (String) -> Unit,
    onRemoveRecent: ((String) -> Unit)?,
    keyPrefix: String,
    modifier: Modifier = Modifier,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 44.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (entries.isEmpty() && emptyContent != null) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                contentType = "empty_placeholder",
            ) {
                emptyContent()
            }
        } else {
            items(
                items = entries,
                key = { "${keyPrefix}_${it.emoji.key}" },
                contentType = { "emoji_tile" },
            ) { entry ->
                val tileEmoji = if (!isRecent && entry.hasVariants && stagedEmoji != null) {
                    entry.variants.firstOrNull { it.emoji == stagedEmoji }?.emoji ?: entry.displayEmoji
                } else {
                    entry.displayEmoji
                }
                EmojiTile(
                    emoji = tileEmoji,
                    variants = entry.variants,
                    selected = stagedEmoji != null && tileEmoji == stagedEmoji,
                    hasVariants = !isRecent && entry.hasVariants,
                    onClick = onTileClick,
                    onRemoveFromRecents = onRemoveRecent?.let { callback ->
                        { callback(tileEmoji) }
                    },
                )
            }
        }
    }
}
