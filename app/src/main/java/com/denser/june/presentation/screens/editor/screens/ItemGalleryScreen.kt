package com.denser.june.presentation.screens.editor.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.editor.EditorAction
import com.denser.june.presentation.screens.editor.components.AddLocationDialog
import com.denser.june.presentation.screens.editor.components.JournalMapItem
import com.denser.june.presentation.screens.editor.components.JournalMediaItem
import com.denser.june.presentation.screens.editor.components.JournalSongItem
import com.denser.june.presentation.screens.editor.components.MediaOperations
import com.denser.june.presentation.screens.editor.EditorVM
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemGalleryScreen(
    viewModel: EditorVM
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    var showLocationDialog by remember { mutableStateOf(false) }

    val mediaOperations = remember(state.images) {
        MediaOperations(
            onRemoveMedia = { viewModel.onAction(EditorAction.RemoveImage(it)) },
            onMoveToFront = { viewModel.onAction(EditorAction.MoveImageToFront(it)) },
            onMediaClick = { path ->
                navigator.navigateTo(
                    Route.JournalMediaDetail(
                        journalId = state.journalId ?: "",
                        initialIndex = state.images.reversed().indexOf(path)
                    ),
                    isSingleTop = true,
                )
            },
            frontMediaPath = state.images.lastOrNull(),
            onRemoveSong = { viewModel.onAction(EditorAction.RemoveSong) },
            onSongSheetToggle = { navigator.navigateTo(Route.AddSong, isSingleTop = true) },
            onRemoveLocation = { viewModel.onAction(EditorAction.RemoveLocation) },
            onLocationDialogToggle = { showLocationDialog = true },
        )
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = { Text(stringResource(R.string.journal_media)) },
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
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + padding.calculateBottomPadding()
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.songDetails != null) {
                item(key = "song_card", span = { GridItemSpan(2) }) {
                    val currentSong = state.songDetails!!
                    val isInLibrary = remember(currentSong, state.librarySongs) {
                        state.librarySongs.any {
                            it.title.equals(currentSong.title, ignoreCase = true) &&
                            it.artistName.equals(currentSong.artistName, ignoreCase = true)
                        }
                    }
                    JournalSongItem(
                        details = currentSong,
                        isFetching = state.isFetchingSong,
                        onRemove = mediaOperations.onRemoveSong,
                        onEdit = { mediaOperations.onSongSheetToggle(true) },
                        onTrim = { viewModel.onAction(EditorAction.OpenClipTrimmer(currentSong)) },
                        isInLibrary = isInLibrary,
                        onAddToLibrary = { viewModel.onAction(EditorAction.AddSongToLibrary(currentSong)) }
                    )
                }
            }
            if (state.location != null) {
                item(key = "map_card", span = { GridItemSpan(2) }) {
                    JournalMapItem(
                        location = state.location!!,
                        onMapClick = { mediaOperations.onLocationDialogToggle(true) },
                        onRemove = mediaOperations.onRemoveLocation
                    )
                }
            }
            itemsIndexed(state.images.reversed()) { _, path ->
                JournalMediaItem(
                    path = path,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    operations = mediaOperations,
                    isLargeItem = false,
                    enablePlayback = false,
                )
            }
        }
    }


    if (showLocationDialog) {
        AddLocationDialog(
            existingLocation = state.location,
            onLocationSelected = { loc ->
                viewModel.onAction(EditorAction.SetLocation(loc))
            },
            onRemoveLocation = {
                viewModel.onAction(EditorAction.RemoveLocation)
            },
            onDismiss = { showLocationDialog = false }
        )
    }
}