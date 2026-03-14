package com.denser.june.presentation.screens.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.utils.toDateWithDay
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.editor.components.JournalItemsPreview
import com.denser.june.presentation.screens.editor.components.MediaOperations
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.hyphen.state.rememberHyphenTextState
import com.denser.hyphen.ui.HyphenStyleConfig
import com.denser.hyphen.ui.material3.HyphenTextEditor

import com.denser.june.R
import com.denser.june.core.utils.toFullTime
import com.denser.june.core.utils.toLocalTime
import com.denser.june.presentation.screens.editor.components.EditorToolbar
import com.denser.june.presentation.utils.TagUtils
import com.denser.june.presentation.utils.UiUtils
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen() {
    val viewModel: EditorVM = koinViewModel()
    val navigator = koinInject<AppNavigator>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val dialogState = rememberEditorDialogState()
    var showMenu by remember { mutableStateOf(false) }
    val isEditorReady = !state.isLoading
    val hyphenState = rememberHyphenTextState()

    LaunchedEffect(isEditorReady) {
        if (isEditorReady && state.content.isNotEmpty()) {
            hyphenState.setMarkdown(state.content)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val contentFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    var isEditorFocused by remember { mutableStateOf(false) }
    val showSaveButton = if (state.isDraft) state.hasContent else state.isDirty

    val formattedDate = remember(state.dateTime) { state.dateTime.toDateWithDay() }
    val formattedTime = remember(state.dateTime) {
        val time = state.dateTime.toLocalTime()
        if (time != LocalTime.MIDNIGHT) time.toFullTime() else null
    }

    val onBack = {
        if (!state.isDraft && state.isDirty) {
            dialogState.showExitDialog = true
        } else {
            viewModel.onAction(EditorAction.NavigateBack)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler { onBack() }

    val mediaOperations = remember(state.images) {
        MediaOperations(
            onItemSheetToggle = { dialogState.showAddItemSheet = it },
            onRemoveMedia = { viewModel.onAction(EditorAction.RemoveImage(it)) },
            onMoveToFront = { viewModel.onAction(EditorAction.MoveImageToFront(it)) },
            onMediaClick = { path ->
                navigator.navigateTo(
                    Route.JournalMediaDetail(
                        journalId = state.journalId ?: 0L,
                        initialIndex = state.images.reversed().indexOf(path)
                    ),
                    isSingleTop = true
                )
            },
            frontMediaPath = state.images.lastOrNull(),
            onRemoveSong = { viewModel.onAction(EditorAction.RemoveSong) },
            onSongSheetToggle = { dialogState.showSongSheet = true },
            onRemoveLocation = { viewModel.onAction(EditorAction.RemoveLocation) },
            onLocationDialogToggle = { dialogState.showLocationDialog = true },
        )
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = {},
                navigationIcon = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { onBack() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.75f
                                )
                            )
                        ) { Icon(painterResource(R.drawable.close_24px), "Close") }

                        FilledIconButton(
                            onClick = { dialogState.showEmojiPicker = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.75F
                                )
                            )
                        ) {
                            if (state.emoji != null) Text(state.emoji!!, fontSize = 22.sp)
                            else Icon(
                                painterResource(if (dialogState.showEmojiPicker) R.drawable.sentiment_very_satisfied_24px_fill else R.drawable.sentiment_very_satisfied_24px),
                                "Add Emoji"
                            )
                        }

                        FilledIconButton(
                            onClick = { dialogState.showAddItemSheet = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.75F
                                )
                            )
                        ) {
                            Icon(
                                painterResource(if (dialogState.showAddItemSheet) R.drawable.add_circle_24px_fill else R.drawable.add_circle_24px),
                                "Add Attachment"
                            )
                        }

                        FilledIconButton(
                            onClick = { dialogState.showTagsDialog = true },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.75F
                                )
                            )
                        ) {
                            Icon(
                                painterResource(if (dialogState.showTagsDialog) R.drawable.sell_24px_fill else R.drawable.sell_24px),
                                "Add Tags"
                            )
                        }
                    }
                },
                actions = {
                    if (showSaveButton) {
                        Button(
                            enabled = !state.isLoading,
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                if (!state.isLoading) viewModel.onAction(EditorAction.SaveJournal)
                            },
                        ) {
                            Text("Save")
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.onAction(EditorAction.ToggleBookmark) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.75f
                                )
                            ),
                        ) {
                            Icon(
                                painterResource(if (state.isBookmarked) R.drawable.bookmark_added_24px_fill else R.drawable.bookmark_24px),
                                "Toggle Bookmark"
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.75f
                                )
                            ),
                        ) {
                            Icon(
                                painterResource(R.drawable.more_vert_24px),
                                "Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 3.dp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp)
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    dialogState.showDeleteConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.delete_24px),
                                        null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isEditorFocused) {
                EditorToolbar(
                    state = hyphenState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { contentFocusRequester.requestFocus() }
                )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
            ) {
                if (state.images.isNotEmpty() || state.songDetails != null || state.location != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    JournalItemsPreview(
                        mediaPaths = state.images,
                        mediaOperations = mediaOperations,
                        songDetails = state.songDetails,
                        location = state.location,
                        onShowAllClick = {
                            navigator.navigateTo(
                                Route.JournalMedia(journalId = state.journalId ?: 0L),
                                isSingleTop = true
                            )
                        }
                    )
                }

                TextField(
                    value = state.title,
                    onValueChange = { viewModel.onAction(EditorAction.ChangeTitle(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Add title",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = UiUtils.getTransparentTextFieldColors()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            dialogState.showDatePicker = true
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.today_24px),
                        null,
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formattedDate,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (formattedTime != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painterResource(R.drawable.schedule_24px),
                            null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formattedTime,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.tags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dialogState.showTagsDialog = true },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.tags) { tag ->
                            SuggestionChip(
                                onClick = { dialogState.showTagsDialog = true },
                                label = { Text(tag, fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp),
                                border = null,
                                colors = TagUtils.getTagSuggestionChipColors(tag)
                            )
                        }
                    }
                }

                HyphenTextEditor(
                    state = hyphenState,
                    onMarkdownChange = {
                        viewModel.onAction(EditorAction.ChangeContent(it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 84.dp)
                        .focusRequester(contentFocusRequester)
                        .onFocusChanged { focusState ->
                            isEditorFocused = focusState.isFocused
                        },
                    placeholder = {
                        Text(
                            "What's on your mind?",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    colors = UiUtils.getTransparentTextFieldColors().copy(
                        unfocusedPlaceholderColor =  MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedPlaceholderColor =  MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    ),
                    styleConfig = HyphenStyleConfig(
                        boldStyle = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    EditorModals(
        dialogState = dialogState,
        editorState = state,
        onAction = viewModel::onAction
    )
}