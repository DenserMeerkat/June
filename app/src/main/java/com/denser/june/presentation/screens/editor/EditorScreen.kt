package com.denser.june.presentation.screens.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.media.compress.CompressUtils
import com.denser.june.core.domain.model.enums.EditorLayoutDirection
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.utils.toDateWithDay
import com.denser.june.core.utils.toFullTime
import com.denser.june.core.utils.toLocalTime
import com.denser.june.core.utils.LanguageHelper
import com.denser.june.presentation.components.ExportJournalBottomSheet
import com.denser.june.presentation.components.font.FontCustomizationPanel
import com.denser.june.presentation.components.color.ColorCustomizationPanel
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.editor.components.*
import com.denser.june.presentation.screens.home.components.JournalOptionsSheet
import com.denser.june.presentation.theme.JuneThemeWrapper
import com.denser.june.presentation.utils.ExtractedMediaColor
import com.denser.june.presentation.utils.ExtractedMediaTheme
import com.denser.june.presentation.utils.MediaColorExtractor
import com.denser.june.presentation.theme.LocalAppTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import com.denser.june.core.domain.model.FontPresets
import com.denser.june.presentation.components.color.ColorScreenTab
import com.denser.june.presentation.theme.getJournalFontFamily
import com.denser.june.presentation.utils.UiUtils
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalTime

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun EditorScreen() {
    val viewModel: EditorVM = koinViewModel()
    val navigator = koinInject<AppNavigator>()
    val journalPreferences = koinInject<JournalPreferences>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isMarkdownEnabled by journalPreferences.isMarkdownEnabled()
        .collectAsStateWithLifecycle(initialValue = true)
    val isKeyboardAutocorrectEnabled by journalPreferences.isKeyboardAutocorrectEnabled()
        .collectAsStateWithLifecycle(initialValue = true)
    val isKeyboardCapitalizationEnabled by journalPreferences.isKeyboardCapitalizationEnabled()
        .collectAsStateWithLifecycle(initialValue = true)
    val editorLayoutDirection by journalPreferences.editorLayoutDirection()
        .collectAsStateWithLifecycle(initialValue = EditorLayoutDirection.AUTO)
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val dialogState = rememberEditorDialogState()
    var showOptionsSheet by remember { mutableStateOf(false) }
    val hyphenState = viewModel.hyphenState

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

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val contentFocusRequester = remember { FocusRequester() }


    var isEditorFocused by remember { mutableStateOf(false) }
    val showSaveButton = if (state.isDraft) state.hasContent else state.isDirty

    var isFontMode by remember { mutableStateOf(false) }
    var isColorMode by remember { mutableStateOf(false) }
    var editorColorTab by rememberSaveable { mutableStateOf(ColorScreenTab.COLOR) }
    var previewSlideIndex by rememberSaveable { mutableIntStateOf(0) }

    var extractedMediaColors by remember { mutableStateOf<List<ExtractedMediaColor>>(emptyList()) }
    var extractedMediaThemes by remember { mutableStateOf<List<ExtractedMediaTheme>>(emptyList()) }

    LaunchedEffect(state.images, state.songDetails?.localThumbnailPath) {
        val songThumbnail = state.songDetails?.localThumbnailPath
        extractedMediaColors = MediaColorExtractor.extractColors(
            context = context,
            imagePaths = state.images,
            songThumbnailPath = songThumbnail
        )
        extractedMediaThemes = MediaColorExtractor.extractThemes(
            context = context,
            imagePaths = state.images,
            songThumbnailPath = songThumbnail
        )
    }

    val formattedDate = remember(state.dateTime) { state.dateTime.toDateWithDay() }
    val formattedTime = remember(state.dateTime, state.timeFormat) {
        val time = state.dateTime.toLocalTime()
        if (time != LocalTime.MIDNIGHT) time.toFullTime(is24Hour = state.timeFormat == TimeFormat.TWENTY_FOUR_HOUR) else null
    }

    val onBack = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(EditorAction.Refresh)
    }

    BackHandler {
        if (isFontMode) {
            isFontMode = false
        } else if (isColorMode) {
            isColorMode = false
        } else {
            onBack()
        }
    }

    val isCustomizing = isFontMode || isColorMode

    val mediaOperations = remember(state.images, isCustomizing) {
        MediaOperations(
            onItemSheetToggle = { if (!isCustomizing) dialogState.showAddItemSheet = it },
            onRemoveMedia = {
                if (!isCustomizing) {
                    CompressUtils.cleanUncompressedOriginalFile(context, it)
                    viewModel.onAction(EditorAction.RemoveImage(it))
                }
            },
            onMoveToFront = { if (!isCustomizing) viewModel.onAction(EditorAction.MoveImageToFront(it)) },
            onMediaClick = { path ->
                if (!isCustomizing) {
                    navigator.navigateTo(
                        Route.JournalMediaDetail(
                            journalId = state.journalId ?: "",
                            initialIndex = state.images.reversed().indexOf(path),
                            dateTime = state.dateTime
                        ),
                        isSingleTop = true
                    )
                }
            },
            frontMediaPath = state.images.lastOrNull(),
            onRemoveSong = { if (!isCustomizing) viewModel.onAction(EditorAction.RemoveSong) },
            onSongSheetToggle = { if (!isCustomizing) navigator.navigateTo(Route.AddSong, isSingleTop = true) },
            onTrimSong = {
                if (!isCustomizing) {
                    val song = state.songDetails ?: return@MediaOperations
                    viewModel.onAction(EditorAction.OpenClipTrimmer(song))
                    navigator.navigateTo(Route.AddSong, isSingleTop = true)
                }
            },
            onEditJournalSong = {
                if (!isCustomizing) {
                    viewModel.onAction(EditorAction.OpenJournalSongEdit)
                    navigator.navigateTo(Route.AddSong, isSingleTop = true)
                }
            },
            onRemoveLocation = { if (!isCustomizing) viewModel.onAction(EditorAction.RemoveLocation) },
            onLocationDialogToggle = { if (!isCustomizing) dialogState.showLocationDialog = true },
        )
    }

    JuneThemeWrapper(
        theme = state.theme,
        font = state.font,
        fontVariation = state.fontVariation
    ) {
        Scaffold(
            topBar = {
                EditorTopAppBar(
                    isFontMode = isFontMode,
                    isColorMode = isColorMode,
                    showAddItemSheet = dialogState.showAddItemSheet,
                    showSaveButton = showSaveButton,
                    isLoading = state.isLoading,
                    onCloseClick = {
                        if (isFontMode) isFontMode = false
                        else if (isColorMode) isColorMode = false
                        else onBack()
                    },
                    isFontDirty = state.isFontDirty,
                    onResetFont = {
                        viewModel.onAction(EditorAction.SetJournalFont(state.initialFont, state.initialFontVariation))
                    },
                    isThemeDirty = state.isThemeDirty,
                    onResetTheme = {
                        viewModel.onAction(EditorAction.SetJournalTheme(state.initialTheme))
                    },
                    onFontModeToggle = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        isColorMode = false
                        isFontMode = !isFontMode
                    },
                    onColorModeToggle = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        isFontMode = false
                        isColorMode = !isColorMode
                    },
                    onAddItemClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        dialogState.showAddItemSheet = true
                    },
                    onSaveClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        if (!state.isLoading) {
                            state.images.forEach { CompressUtils.cleanUncompressedOriginalFile(context, it) }
                            viewModel.onAction(EditorAction.SaveJournal)
                        }
                    },
                    onOptionsClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        showOptionsSheet = true
                    }
                )
            },
            bottomBar = {},
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .imePadding()
            ) {
            val availableHeight = maxHeight
            var fixedContentHeight by remember { mutableStateOf(0.dp) }
            var toolbarHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            val journalFontFamily = getJournalFontFamily(
                fontName = state.font,
                variation = state.fontVariation
            )

            val fontAnimDuration = 300
            val fontEasing = FastOutSlowInEasing
            val fontEnterTransition = remember {
                slideInVertically(
                    animationSpec = tween(fontAnimDuration, easing = fontEasing),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(fontAnimDuration, easing = fontEasing))
            }
            val fontExitTransition = remember {
                slideOutVertically(
                    animationSpec = tween(fontAnimDuration, easing = fontEasing),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = tween(fontAnimDuration, easing = fontEasing))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    fixedContentHeight = with(density) { size.height.toDp() }
                                }
                        ) {
                            AnimatedVisibility(
                                visible = !isFontMode,
                                enter = expandVertically(
                                    animationSpec = tween(fontAnimDuration, easing = fontEasing)
                                ) + fadeIn(animationSpec = tween(fontAnimDuration, easing = fontEasing)),
                                exit = shrinkVertically(
                                    animationSpec = tween(fontAnimDuration, easing = fontEasing)
                                ) + fadeOut(animationSpec = tween(fontAnimDuration, easing = fontEasing))
                            ) {
                                if (state.images.isNotEmpty() || state.songDetails != null || state.location != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    JournalItemsPreview(
                                        mediaPaths = state.images,
                                        mediaOperations = mediaOperations,
                                        songDetails = state.songDetails,
                                        location = state.location,
                                        processingMediaPaths = state.processingMediaPaths,
                                        readOnly = isCustomizing,
                                        selectedSlideIndex = previewSlideIndex,
                                        onSlideIndexChange = { previewSlideIndex = it },
                                        theme = state.theme,
                                        onShowAllClick = {
                                            if (!isCustomizing) {
                                                navigator.navigateTo(
                                                    Route.JournalMedia(journalId = state.journalId ?: ""),
                                                    isSingleTop = true
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            val editorDirection = when (editorLayoutDirection) {
                                EditorLayoutDirection.AUTO -> if (LanguageHelper.isCurrentLocaleRtl()) LayoutDirection.Rtl else LayoutDirection.Ltr
                                EditorLayoutDirection.LTR -> LayoutDirection.Ltr
                                EditorLayoutDirection.RTL -> LayoutDirection.Rtl
                            }

                            val titleTextStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = journalFontFamily ?: MaterialTheme.typography.headlineMedium.fontFamily
                            )

                            CompositionLocalProvider(LocalLayoutDirection provides editorDirection) {
                                TextField(
                                    value = state.title,
                                    onValueChange = { viewModel.onAction(EditorAction.ChangeTitle(it)) },
                                    readOnly = isCustomizing,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.add_title),
                                            style = titleTextStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = if (isKeyboardCapitalizationEnabled) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                                        autoCorrectEnabled = isKeyboardAutocorrectEnabled
                                    ),
                                    textStyle = titleTextStyle,
                                    colors = UiUtils.getTransparentTextFieldColors()
                                )
                            }

                            AnimatedVisibility(
                                visible = !isFontMode,
                                enter = expandVertically(
                                    animationSpec = tween(fontAnimDuration, easing = fontEasing)
                                ) + fadeIn(animationSpec = tween(fontAnimDuration, easing = fontEasing)),
                                exit = shrinkVertically(
                                    animationSpec = tween(fontAnimDuration, easing = fontEasing)
                                ) + fadeOut(animationSpec = tween(fontAnimDuration, easing = fontEasing))
                            ) {
                                EditorMetadataBar(
                                    emoji = state.emoji,
                                    formattedDate = formattedDate,
                                    formattedTime = formattedTime,
                                    tagCount = state.tags.size,
                                    onEmojiClick = {
                                        if (!isCustomizing) {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            dialogState.showEmojiPicker = true
                                        }
                                    },
                                    onDatePickerOpen = {
                                        if (!isCustomizing) {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            dialogState.datePickerInitialTab = 0
                                            dialogState.showDatePicker = true
                                        }
                                    },
                                    onTimePickerOpen = {
                                        if (!isCustomizing) {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            dialogState.datePickerInitialTab = 1
                                            dialogState.showDatePicker = true
                                        }
                                    },
                                    onTagsClick = {
                                        if (!isCustomizing) {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            dialogState.showTagsDialog = true
                                        }
                                    }
                                )
                            }
                        }

                        JournalContentEditor(
                            state = hyphenState,
                            rawContent = state.content,
                            onMarkdownChange = { newContent ->
                                if (newContent != state.content) {
                                    viewModel.onAction(EditorAction.ChangeContent(newContent))
                                }
                            },
                            onFocusChanged = { isEditorFocused = it },
                            focusRequester = contentFocusRequester,
                            isLoading = state.isLoading,
                            isMarkdownEnabled = isMarkdownEnabled,
                            isKeyboardAutocorrectEnabled = isKeyboardAutocorrectEnabled,
                            isKeyboardCapitalizationEnabled = isKeyboardCapitalizationEnabled,
                            editorLayoutDirection = editorLayoutDirection,
                            fontFamily = journalFontFamily,
                            readOnly = isFontMode || isColorMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(
                                    minHeight = (availableHeight - fixedContentHeight - toolbarHeight).coerceAtLeast(
                                        84.dp
                                    )
                                )
                                .padding(
                                    bottom = with(density) {
                                        val halfLineOffset = (MaterialTheme.typography.bodyLarge.lineHeight / 2).toDp()
                                        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                        when {
                                            isFontMode || isColorMode -> 380.dp + navBarBottom + halfLineOffset
                                            WindowInsets.isImeVisible -> 16.dp + halfLineOffset
                                            else -> 16.dp + navBarBottom
                                        }
                                    }
                                )
                        )
                    }

                    if (!isFontMode && !isColorMode && isEditorFocused && isMarkdownEnabled) {
                        EditorToolbar(
                            state = hyphenState,
                            activeTrigger = activeTrigger,
                            tagSuggestions = state.tagSuggestions,
                            currentTags = state.tags,
                            onTagSelect = onTagSelect,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    toolbarHeight = with(density) { size.height.toDp() }
                                }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isFontMode,
                    enter = fontEnterTransition,
                    exit = fontExitTransition,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    FontCustomizationPanel(
                        selectedFontName = state.font,
                        currentVariation = state.fontVariation,
                        onFontSelect = { newFont ->
                            val defaultVar = newFont?.let { FontPresets.getDefaultVariation(it) }
                            viewModel.onAction(EditorAction.SetJournalFont(newFont, defaultVar))
                        },
                        onVariationChange = { newVariation ->
                            viewModel.onAction(EditorAction.SetJournalFont(state.font, newVariation))
                        },
                        showAppDefault = true,
                        contentHeight = 360.dp
                    )
                }

                AnimatedVisibility(
                    visible = isColorMode,
                    enter = fontEnterTransition,
                    exit = fontExitTransition,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ColorCustomizationPanel(
                        journalTheme = state.theme,
                        extractedMediaColors = extractedMediaColors,
                        extractedMediaThemes = extractedMediaThemes,
                        onThemeChange = { newTheme ->
                            viewModel.onAction(EditorAction.SetJournalTheme(newTheme))
                        },
                        showAppDefault = true,
                        contentHeight = 360.dp,
                        selectedTab = editorColorTab,
                        onTabSelect = { editorColorTab = it }
                    )
                }
            }
        }
        }

        EditorModals(
            dialogState = dialogState,
            editorState = state,
            onAction = viewModel::onAction
        )

        var journalToExport by remember { mutableStateOf<Journal?>(null) }

        ExportJournalBottomSheet(
            journal = journalToExport,
            onDismiss = { journalToExport = null }
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
                    onExportMarkdown = {
                        showOptionsSheet = false
                        journalToExport = journalPreview
                    },
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
}