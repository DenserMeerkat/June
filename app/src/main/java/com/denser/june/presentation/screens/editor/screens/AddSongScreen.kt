package com.denser.june.presentation.screens.editor.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.components.InternetRestrictedBanner
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.editor.EditorAction
import com.denser.june.presentation.screens.editor.EditorVM
import com.denser.june.presentation.screens.editor.components.ActiveJournalSongBar
import com.denser.june.presentation.screens.editor.components.EditSongScope
import com.denser.june.presentation.screens.editor.components.EditSongView
import com.denser.june.presentation.screens.editor.components.LibrarySongRow
import com.denser.june.presentation.screens.editor.components.SongInputCard
import com.denser.june.presentation.screens.editor.components.TrimmerStepView
import com.denser.june.presentation.screens.editor.components.UnimportedSongRow
import com.denser.june.presentation.screens.editor.components.UnimportedSongsHeader
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class AddSongStep {
    Library,
    Trimmer,
    Edit,
    EditJournal
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongScreen(
    viewModel: EditorVM,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val isInternetAllowed by privacyPreferences.getIsInternetAllowedFlow()
        .collectAsStateWithLifecycle(initialValue = true)

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(AddSongStep.Library) }
    var activeTrimmerSong by remember { mutableStateOf<SongDetails?>(null) }
    var activeEditingSong by remember { mutableStateOf<SongDetails?>(null) }

    var showLinkInput by remember { mutableStateOf(false) }
    var songLink by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAction(EditorAction.AttachLocalSong(uri))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(state.isFetchingSong) {
        if (state.isFetchingSong) {
            showLinkInput = true
        }
    }

    LaunchedEffect(state.clipTrimmerSong) {
        val trimming = state.clipTrimmerSong
        if (trimming != null) {
            activeTrimmerSong = trimming
            currentStep = AddSongStep.Trimmer
        }
    }

    val trimmerSaveClip: (SongDetails, Long, Long?) -> Unit = { song, startMs, endMs ->
        viewModel.onAction(EditorAction.SaveClip(startMs, endMs, song))
        onNavigateBack()
    }

    val trimmerDismiss: () -> Unit = {
        viewModel.onAction(EditorAction.DismissClipTrimmer)
        activeTrimmerSong = null
        currentStep = AddSongStep.Library
    }

    BackHandler {
        when (currentStep) {
            AddSongStep.Trimmer -> trimmerDismiss()
            AddSongStep.Edit, AddSongStep.EditJournal -> {
                activeEditingSong = null
                currentStep = AddSongStep.Library
            }
            AddSongStep.Library -> onNavigateBack()
        }
    }

    var editTitle by remember(activeEditingSong) { mutableStateOf(activeEditingSong?.title ?: "") }
    var editArtist by remember(activeEditingSong) { mutableStateOf(activeEditingSong?.artistName ?: "") }
    var editAlbum by remember(activeEditingSong) { mutableStateOf(activeEditingSong?.albumName ?: "") }
    var editGenre by remember(activeEditingSong) { mutableStateOf(activeEditingSong?.genre ?: "") }
    var editArtPath by remember(activeEditingSong) { mutableStateOf(activeEditingSong?.localThumbnailPath) }
    var isArtRemoved by remember(activeEditingSong) { mutableStateOf(false) }

    LaunchedEffect(state.journalEditSongPending) {
        if (state.journalEditSongPending && state.songDetails != null) {
            activeEditingSong = state.songDetails
            currentStep = AddSongStep.EditJournal
            viewModel.onAction(EditorAction.DismissJournalSongEdit)
        }
    }

    var pendingTrimmerSave by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            JuneTopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            AddSongStep.Library -> "Add Song"
                            AddSongStep.Trimmer -> "Trim Song"
                            AddSongStep.Edit -> "Edit Song"
                            AddSongStep.EditJournal -> "Edit Song"
                        }
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        onClick = {
                            when (currentStep) {
                                AddSongStep.Trimmer -> trimmerDismiss()
                                AddSongStep.Edit, AddSongStep.EditJournal -> {
                                    activeEditingSong = null
                                    currentStep = AddSongStep.Library
                                }
                                AddSongStep.Library -> onNavigateBack()
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    when (currentStep) {
                        AddSongStep.Library -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { showLinkInput = !showLinkInput },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (showLinkInput) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = if (showLinkInput) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.link_24px),
                                        contentDescription = "Song Link"
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = { audioFilePicker.launch("audio/*") },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.folder_open_24px),
                                        contentDescription = "Pick Local File"
                                    )
                                }
                            }
                        }

                        AddSongStep.Edit -> {
                            FilledTonalIconButton(
                                onClick = { showDeleteConfirm = true },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete_24px),
                                    contentDescription = "Remove from Library",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        AddSongStep.EditJournal -> Unit
                        AddSongStep.Trimmer -> Unit
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            when (currentStep) {
                AddSongStep.Trimmer -> {
                    JuneFloatingActionBar {
                        JuneFloatingAction(
                            onClick = { trimmerDismiss() },
                            label = "Cancel",
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = null
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        JuneFloatingAction(
                            onClick = { pendingTrimmerSave?.invoke() },
                            label = "Done",
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.check_24px),
                                    contentDescription = null
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                AddSongStep.Edit -> {
                    JuneFloatingActionBar {
                        JuneFloatingAction(
                            onClick = {
                                activeEditingSong = null
                                currentStep = AddSongStep.Library
                            },
                            label = "Cancel",
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = null
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        JuneFloatingAction(
                            onClick = {
                                val orig = activeEditingSong ?: return@JuneFloatingAction
                                val updated = buildUpdatedSong(
                                    orig = orig,
                                    title = editTitle,
                                    artist = editArtist,
                                    album = editAlbum,
                                    genre = editGenre,
                                    artPath = editArtPath,
                                    isArtRemoved = isArtRemoved
                                )
                                viewModel.onAction(EditorAction.SaveLibrarySongMeta(orig, updated))
                                activeEditingSong = null
                                currentStep = AddSongStep.Library
                            },
                            label = "Save",
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.check_24px),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                AddSongStep.EditJournal -> {
                    JuneFloatingActionBar {
                        JuneFloatingAction(
                            onClick = {
                                activeEditingSong = null
                                currentStep = AddSongStep.Library
                            },
                            label = "Cancel",
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = null
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        JuneFloatingAction(
                            onClick = {
                                val orig = activeEditingSong ?: return@JuneFloatingAction
                                val updated = buildUpdatedSong(
                                    orig = orig,
                                    title = editTitle,
                                    artist = editArtist,
                                    album = editAlbum,
                                    genre = editGenre,
                                    artPath = editArtPath,
                                    isArtRemoved = isArtRemoved
                                )
                                viewModel.onAction(EditorAction.SaveJournalSongMeta(updated))
                                activeEditingSong = null
                                currentStep = AddSongStep.Library
                            },
                            label = "Save",
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.check_24px),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                AddSongStep.Library -> Unit
            }
        },
        bottomBar = {
            val attachedSong = state.songDetails
            if (currentStep == AddSongStep.Library && attachedSong != null) {
                ActiveJournalSongBar(
                    song = attachedSong,
                    onEdit = {
                        activeEditingSong = attachedSong
                        editTitle = attachedSong.title
                        editArtist = attachedSong.artistName
                        editAlbum = attachedSong.albumName ?: ""
                        editGenre = attachedSong.genre ?: ""
                        editArtPath = attachedSong.localThumbnailPath
                        isArtRemoved = false
                        currentStep = AddSongStep.EditJournal
                    },
                    onTrim = {
                        activeTrimmerSong = attachedSong
                        viewModel.onAction(EditorAction.OpenClipTrimmer(attachedSong))
                        currentStep = AddSongStep.Trimmer
                    },
                    onRemove = { viewModel.onAction(EditorAction.RemoveSong) }
                )
            }
        }
    ) { paddingValues ->
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Remove Song?") },
                text = { Text("Are you sure you want to remove this song from your library?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            activeEditingSong?.let {
                                viewModel.onAction(EditorAction.RemoveLibrarySong(it))
                            }
                            activeEditingSong = null
                            currentStep = AddSongStep.Library
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        when (currentStep) {
            AddSongStep.Library -> {
                val density = LocalDensity.current
                var overlayHeightPx by remember { mutableIntStateOf(0) }
                val overlayHeightDp = with(density) { overlayHeightPx.toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "overlay_top_spacer") {
                            val animatedSpacerHeight by animateDpAsState(
                                targetValue = if (showLinkInput) overlayHeightDp else 0.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "overlaySpacer"
                            )
                            if (animatedSpacerHeight > 0.dp) {
                                Spacer(modifier = Modifier.height(animatedSpacerHeight))
                            }
                        }

                        if (!isInternetAllowed) {
                            item {
                                InternetRestrictedBanner(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp)
                                )
                            }
                        }

                        if (state.librarySongs.isEmpty() && state.unimportedJournalSongs.isEmpty()) {
                            item {
                                JunePlaceholderPage(
                                    icon = R.drawable.music_note_24px,
                                    title = stringResource(R.string.empty_library),
                                    subtitle = stringResource(R.string.empty_library_desc),
                                    fillMaxSize = false,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        } else {
                            if (state.librarySongs.isNotEmpty()) {
                                items(
                                    items = state.librarySongs,
                                    key = { "${it.title}_${it.artistName}_${it.localPreviewPath ?: it.previewUrl ?: ""}" }
                                ) { song ->
                                    val isCurrentlyAttached =
                                        state.songDetails?.title == song.title && state.songDetails?.artistName == song.artistName
                                    LibrarySongRow(
                                        song = song,
                                        isCurrentlyAttached = isCurrentlyAttached,
                                        onClick = {
                                            activeTrimmerSong = song
                                            viewModel.onAction(EditorAction.OpenClipTrimmer(song))
                                            currentStep = AddSongStep.Trimmer
                                        },
                                        onEdit = {
                                            activeEditingSong = song
                                            editTitle = song.title
                                            editArtist = song.artistName
                                            editAlbum = song.albumName ?: ""
                                            editGenre = song.genre ?: ""
                                            editArtPath = song.localThumbnailPath
                                            isArtRemoved = false
                                            currentStep = AddSongStep.Edit
                                        },
                                        onRemove = {
                                            viewModel.onAction(EditorAction.RemoveLibrarySong(song))
                                        }
                                    )
                                }
                            }

                            if (state.unimportedJournalSongs.isNotEmpty()) {
                                item {
                                    UnimportedSongsHeader(
                                        isAnyImporting = state.importingSongKeys.isNotEmpty(),
                                        onAddAll = {
                                            viewModel.onAction(EditorAction.AddAllSongsToLibrary(state.unimportedJournalSongs))
                                        }
                                    )
                                }

                                items(
                                    items = state.unimportedJournalSongs,
                                    key = { "unimported_${it.title}_${it.artistName}" }
                                ) { unimportedSong ->
                                    val songKey = "${unimportedSong.title.trim().lowercase()}_${unimportedSong.artistName.trim().lowercase()}"
                                    val isImporting = songKey in state.importingSongKeys

                                    UnimportedSongRow(
                                        song = unimportedSong,
                                        isImporting = isImporting,
                                        onAdd = {
                                            viewModel.onAction(EditorAction.AddSongToLibrary(unimportedSong))
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }

                    AnimatedVisibility(
                        visible = showLinkInput,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .onSizeChanged { overlayHeightPx = it.height }
                    ) {
                        SongInputCard(
                            songLink = songLink,
                            onLinkChange = { songLink = it },
                            isFetching = state.isFetchingSong,
                            fetchProgress = state.songFetchProgress,
                            enabled = isInternetAllowed,
                            onPaste = {
                                scope.launch {
                                    val clipText = clipboard.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        songLink = clipText.trim()
                                        viewModel.onAction(EditorAction.FetchSong(clipText.trim()))
                                    }
                                }
                            },
                            onFetch = {
                                if (songLink.isNotBlank()) {
                                    viewModel.onAction(EditorAction.FetchSong(songLink.trim()))
                                }
                            }
                        )
                    }
                }
            }

            AddSongStep.Trimmer -> {
                activeTrimmerSong?.let { song ->
                    TrimmerStepView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        songDetails = song,
                        onBindSaveAction = { pendingTrimmerSave = it },
                        onSaveClip = trimmerSaveClip
                    )
                }
            }

            AddSongStep.Edit -> {
                activeEditingSong?.let { songToEdit ->
                    EditSongView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        song = songToEdit,
                        editScope = EditSongScope.Library,
                        title = editTitle,
                        onTitleChange = { editTitle = it },
                        artist = editArtist,
                        onArtistChange = { editArtist = it },
                        album = editAlbum,
                        onAlbumChange = { editAlbum = it },
                        genre = editGenre,
                        onGenreChange = { editGenre = it },
                        artPath = editArtPath,
                        isArtRemoved = isArtRemoved,
                        onArtPathChange = {
                            editArtPath = it
                            isArtRemoved = false
                        },
                        onRemoveArt = {
                            editArtPath = null
                            isArtRemoved = true
                        }
                    )
                }
            }

            AddSongStep.EditJournal -> {
                activeEditingSong?.let { songToEdit ->
                    EditSongView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        song = songToEdit,
                        editScope = EditSongScope.JournalOnly,
                        title = editTitle,
                        onTitleChange = { editTitle = it },
                        artist = editArtist,
                        onArtistChange = { editArtist = it },
                        album = editAlbum,
                        onAlbumChange = { editAlbum = it },
                        genre = editGenre,
                        onGenreChange = { editGenre = it },
                        artPath = editArtPath,
                        isArtRemoved = isArtRemoved,
                        onArtPathChange = {
                            editArtPath = it
                            isArtRemoved = false
                        },
                        onRemoveArt = {
                            editArtPath = null
                            isArtRemoved = true
                        },
                        enabled = true
                    )
                }
            }
        }
    }
}

private fun buildUpdatedSong(
    orig: SongDetails,
    title: String,
    artist: String,
    album: String,
    genre: String,
    artPath: String?,
    isArtRemoved: Boolean
): SongDetails {
    return orig.copy(
        title = title.trim().ifEmpty { "Unknown Title" },
        artistName = artist.trim().ifEmpty { "Unknown Artist" },
        albumName = album.trim().ifEmpty { null },
        genre = genre.trim().ifEmpty { null },
        localThumbnailPath = if (isArtRemoved) null else artPath,
        thumbnailUrl = if (isArtRemoved) null else orig.thumbnailUrl
    )
}
