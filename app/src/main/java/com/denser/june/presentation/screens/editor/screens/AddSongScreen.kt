package com.denser.june.presentation.screens.editor.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.model.SongSourceType
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.presentation.components.InternetRestrictedBanner
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.RestrictedAsyncImage
import com.denser.june.presentation.screens.editor.EditorAction
import com.denser.june.presentation.screens.editor.EditorVM
import com.denser.june.presentation.screens.editor.components.EditSongView
import com.denser.june.presentation.screens.editor.components.EmptyLibraryView
import com.denser.june.presentation.screens.editor.components.LibrarySongRow
import com.denser.june.presentation.screens.editor.components.SongInputCard
import com.denser.june.presentation.screens.editor.components.TrimmerStepView
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class AddSongStep {
    Library,
    Trimmer,
    Edit
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongScreen(
    viewModel: EditorVM,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val songRepository = koinInject<SongRepository>()
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
    var isRefetching by remember { mutableStateOf(false) }
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
            AddSongStep.Edit -> {
                if (!isRefetching) {
                    activeEditingSong = null
                    currentStep = AddSongStep.Library
                }
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
                        }
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        onClick = {
                            when (currentStep) {
                                AddSongStep.Trimmer -> trimmerDismiss()
                                AddSongStep.Edit -> {
                                    if (!isRefetching) {
                                        activeEditingSong = null
                                        currentStep = AddSongStep.Library
                                    }
                                }
                                AddSongStep.Library -> onNavigateBack()
                            }
                        },
                        enabled = !isRefetching,
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
                            val songToEdit = activeEditingSong
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (songToEdit?.sourceType == SongSourceType.LINK) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            scope.launch {
                                                isRefetching = true
                                                val res = songRepository.refetchSongDetails(songToEdit)
                                                isRefetching = false
                                                res.onSuccess { fresh ->
                                                    editTitle = fresh.title
                                                    editArtist = fresh.artistName
                                                    editAlbum = fresh.albumName ?: ""
                                                    editGenre = fresh.genre ?: ""
                                                    if (fresh.localThumbnailPath != null) {
                                                        editArtPath = fresh.localThumbnailPath
                                                        isArtRemoved = false
                                                    }
                                                    Toast.makeText(context, "Song details updated", Toast.LENGTH_SHORT).show()
                                                }.onFailure {
                                                    Toast.makeText(context, "Failed to re-fetch details", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = !isRefetching,
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        if (isRefetching) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.sync_24px),
                                                contentDescription = "Re-fetch Details"
                                            )
                                        }
                                    }
                                }

                                FilledTonalIconButton(
                                    onClick = { showDeleteConfirm = true },
                                    enabled = !isRefetching,
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
                        }
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
                                if (!isRefetching) {
                                    activeEditingSong = null
                                    currentStep = AddSongStep.Library
                                }
                            },
                            label = "Cancel",
                            enabled = !isRefetching,
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
                                val updated = orig.copy(
                                    title = editTitle.trim().ifEmpty { "Unknown Title" },
                                    artistName = editArtist.trim().ifEmpty { "Unknown Artist" },
                                    albumName = editAlbum.trim().ifEmpty { null },
                                    genre = editGenre.trim().ifEmpty { null },
                                    localThumbnailPath = if (isArtRemoved) null else editArtPath,
                                    thumbnailUrl = if (isArtRemoved) null else orig.thumbnailUrl
                                )
                                viewModel.onAction(EditorAction.SaveLibrarySongMeta(orig, updated))
                                activeEditingSong = null
                                currentStep = AddSongStep.Library
                            },
                            label = "Save",
                            enabled = !isRefetching,
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
                            activeEditingSong?.let { viewModel.onAction(EditorAction.RemoveLibrarySong(it)) }
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
                            EmptyLibraryView()
                        }
                    } else {
                        if (state.librarySongs.isNotEmpty()) {
                            items(
                                items = state.librarySongs,
                                key = { "${it.title}_${it.artistName}_${it.localPreviewPath ?: it.previewUrl ?: ""}" }
                            ) { song ->
                                val isCurrentlyAttached = state.songDetails?.title == song.title && state.songDetails?.artistName == song.artistName
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "From Your Journals",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    val isAnyImporting = state.importingSongKeys.isNotEmpty()
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.onAction(EditorAction.AddAllSongsToLibrary(state.unimportedJournalSongs))
                                        },
                                        enabled = !isAnyImporting,
                                        modifier = Modifier.height(32.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        if (isAnyImporting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.music_note_add_24px),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = "Add all",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            items(
                                items = state.unimportedJournalSongs,
                                key = { "unimported_${it.title}_${it.artistName}" }
                            ) { unimportedSong ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp)),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            RestrictedAsyncImage(
                                                imageUrl = unimportedSong.thumbnailUrl,
                                                localPath = unimportedSong.localThumbnailPath,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                iconSize = 22.dp,
                                                iconTint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = unimportedSong.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = unimportedSong.artistName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        val songKey = "${unimportedSong.title.trim().lowercase()}_${unimportedSong.artistName.trim().lowercase()}"
                                        val isImporting = songKey in state.importingSongKeys

                                        FilledTonalIconButton(
                                            onClick = { viewModel.onAction(EditorAction.AddSongToLibrary(unimportedSong)) },
                                            modifier = Modifier.size(36.dp),
                                            enabled = !isImporting,
                                            shape = CircleShape
                                        ) {
                                            if (isImporting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(R.drawable.music_note_add_24px),
                                                    contentDescription = "Add to Library",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
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
                        enabled = !isRefetching
                    )
                }
            }
        }
    }
}
