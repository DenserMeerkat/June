package com.denser.june.presentation.screens.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.denser.june.presentation.utils.UiUtils
import kotlinx.coroutines.launch
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val hyphenState = rememberHyphenTextState(
        triggerConfigs = remember {
            listOf(
                TriggerConfig(trigger = "@", scheme = "person"),
                TriggerConfig(trigger = "#", scheme = "topic")
            )
        }
    )

    val activeTrigger = hyphenState.activeTrigger
    val activeTagQuery = activeTrigger
        ?.takeIf { it.config.trigger == "@" || it.config.trigger == "#" }
        ?.let { it.config.trigger + it.query }
    LaunchedEffect(activeTagQuery) {
        viewModel.onAction(EditorAction.SearchTags(activeTagQuery ?: ""))
    }

    val onTagSelect: (String) -> Unit = { tag ->
        val trimmed = tag.trim()
        if (trimmed.isNotBlank() && state.tags.none { it.equals(trimmed, ignoreCase = true) }) {
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
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                showOptionsSheet = true
                            },
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
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = UiUtils.getTransparentTextFieldColors()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            dialogState.datePickerInitialTab = 0
                            dialogState.showDatePicker = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .width(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.today_24px),
                            contentDescription = "Date and Time Picker",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            dialogState.datePickerInitialTab = 0
                            dialogState.showDatePicker = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = ButtonGroupDefaults.connectedMiddleButtonShapes().shape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            dialogState.datePickerInitialTab = 1
                            dialogState.showDatePicker = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                        contentPadding = PaddingValues(horizontal = if (formattedTime != null) 12.dp else 0.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .then(if (formattedTime == null) Modifier.width(38.dp) else Modifier)
                    ) {
                        if (formattedTime == null) {
                            Icon(
                                painterResource(R.drawable.schedule_24px),
                                null,
                                Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            dialogState.showTagsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.tags.isNotEmpty()) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            contentColor = if (state.tags.isNotEmpty()) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = if (state.tags.isNotEmpty()) 12.dp else 0.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .then(if (state.tags.isEmpty()) Modifier.width(38.dp) else Modifier)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.tags.isNotEmpty()) R.drawable.sell_24px_fill else R.drawable.sell_24px
                            ),
                            contentDescription = "Tags",
                            modifier = Modifier.size(16.dp)
                        )
                        if (state.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.tags.size.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                softWrap = false
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