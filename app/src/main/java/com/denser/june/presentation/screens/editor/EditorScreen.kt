package com.denser.june.presentation.screens.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.utils.toDateWithDay
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.home.components.JournalOptionsSheet
import com.denser.june.presentation.screens.editor.components.JournalItemsPreview
import com.denser.june.presentation.screens.editor.components.MediaOperations
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.hyphen.state.rememberHyphenTextState
import com.denser.june.presentation.screens.editor.components.JournalContentEditor
import com.denser.hyphen.model.TriggerConfig

import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.utils.toFullTime
import com.denser.june.core.utils.toLocalTime
import com.denser.june.presentation.screens.editor.components.EditorToolbar
import com.denser.june.presentation.utils.TagUtils
import com.denser.june.presentation.utils.UiUtils
import kotlinx.coroutines.launch
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen() {
    val viewModel: EditorVM = koinViewModel()
    val navigator = koinInject<AppNavigator>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val dialogState = rememberEditorDialogState()
    var showOptionsSheet by remember { mutableStateOf(false) }
    val isEditorReady = !state.isLoading
    val hyphenState = rememberHyphenTextState()

    LaunchedEffect(Unit) {
        hyphenState.triggerConfigs = listOf(
            TriggerConfig(trigger = "@", scheme = "person"),
            TriggerConfig(trigger = "#", scheme = "topic")
        )
    }

    val activeTrigger = hyphenState.activeTrigger
    val activeTagQuery = activeTrigger
        ?.takeIf { it.config.trigger == "@" || it.config.trigger == "#" }
        ?.let { it.config.trigger + it.query }
    LaunchedEffect(activeTagQuery) {
        viewModel.onAction(EditorAction.SearchTags(activeTagQuery ?: ""))
    }

    val onTagSelect: (String) -> Unit = { tag ->
        val trimmed = tag.trim().lowercase()
        if (trimmed.isNotBlank() && trimmed !in state.tags) {
            viewModel.onAction(EditorAction.UpdateTags(state.tags + trimmed))
        }
    }

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
    val formattedTime = remember(state.dateTime, state.timeFormat) {
        val time = state.dateTime.toLocalTime()
        if (time != LocalTime.MIDNIGHT) time.toFullTime(is24Hour = state.timeFormat == TimeFormat.TWENTY_FOUR_HOUR) else null
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
                        journalId = state.journalId ?: "",
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
                    }

                    Box {
                        IconButton(
                            onClick = { showOptionsSheet = true },
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
                    }
                }
            )
        },
        bottomBar = {
            if (isEditorFocused) {
                EditorToolbar(
                    state = hyphenState,
                    activeTrigger = activeTrigger,
                    tagSuggestions = state.tagSuggestions,
                    currentTags = state.tags,
                    onTagSelect = onTagSelect,
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
                                Route.JournalMedia(journalId = state.journalId ?: ""),
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
                                shape = RoundedCornerShape(12.dp),
                                border = null,
                                colors = TagUtils.getTagSuggestionChipColors(tag)
                            )
                        }
                    }
                }

                JournalContentEditor(
                    state = hyphenState,
                    onMarkdownChange = {
                        viewModel.onAction(EditorAction.ChangeContent(it))
                    },
                    onFocusChanged = { isEditorFocused = it },
                    focusRequester = contentFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .defaultMinSize(minHeight = 84.dp)
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

    if (showOptionsSheet) {
        val journalPreview = remember(state) {
            Journal(
                id = state.journalId ?: "",
                title = state.title,
                content = state.content,
                emoji = state.emoji,
                images = state.images,
                location = state.location,
                songDetails = state.songDetails,
                tags = state.tags,
                createdAt = state.createdAt,
                updatedAt = state.updatedAt,
                dateTime = state.dateTime,
                isBookmarked = state.isBookmarked,
                isArchived = state.isArchived,
                isDraft = state.isDraft,
                deletedAt = state.deletedAt,
                syncedAt = state.syncedAt,
                cloudId = state.cloudId
            )
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            JournalOptionsSheet(
                journal = journalPreview,
                is24Hour = state.timeFormat == TimeFormat.TWENTY_FOUR_HOUR,
                onToggleBookmark = { viewModel.onAction(EditorAction.ToggleBookmark) },
                onDeleteOrRestore = {
                    if (state.isDeleted) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showOptionsSheet = false
                            viewModel.onAction(EditorAction.RestoreJournal)
                        }
                    } else {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showOptionsSheet = false
                            dialogState.showDeleteConfirmation = true
                        }
                    }
                }
            )
        }
    }
}