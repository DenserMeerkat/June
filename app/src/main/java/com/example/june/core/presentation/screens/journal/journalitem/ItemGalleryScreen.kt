package com.example.june.core.presentation.screens.journal.journalitem

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.R
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.components.JuneTopAppBar
import com.example.june.core.presentation.screens.journal.JournalAction
import com.example.june.core.presentation.screens.journal.components.AddLocationDialog
import com.example.june.core.presentation.screens.journal.components.AddSongSheet
import com.example.june.core.presentation.screens.journal.components.JournalMapItem
import com.example.june.core.presentation.screens.journal.components.JournalMediaItem
import com.example.june.core.presentation.screens.journal.components.JournalSongItem
import com.example.june.core.presentation.screens.journal.components.MediaOperations
import com.example.june.viewmodels.JournalVM
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemGalleryScreen(
    viewModel: JournalVM
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()

    var showSongSheet by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val mediaOperations = remember(state.isEditMode, state.images) {
        MediaOperations(
            onRemoveMedia = { viewModel.onAction(JournalAction.RemoveImage(it)) },
            onMoveToFront = { viewModel.onAction(JournalAction.MoveImageToFront(it)) },
            onMediaClick = { path ->
                navigator.navigateTo(
                    Route.JournalMediaDetail(
                        journalId = state.journalId ?: 0L,
                        initialIndex = state.images.reversed().indexOf(path)
                    )
                )
            },
            frontMediaPath = state.images.lastOrNull(),
            onRemoveSong = { viewModel.onAction(JournalAction.RemoveSong) },
            onSongSheetToggle = { showSongSheet = true },
            onRemoveLocation = { viewModel.onAction(JournalAction.RemoveLocation) },
            onLocationDialogToggle = { showLocationDialog = true },
            isEditMode = state.isEditMode,
        )
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = { Text("Journal Media") },
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
                },
                actions = {
                    if (state.isEditMode) {
                        Button(
                            onClick = { viewModel.onAction(JournalAction.SetEditMode(false)) }
                        ) { Text("Done") }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.onAction(JournalAction.SetEditMode(true)) }
                        ) { Text("Edit") }
                    }
                },

                )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.songDetails != null) {
                item(key = "song_card", span = { GridItemSpan(2) }) {
                    JournalSongItem(
                        details = state.songDetails!!,
                        isFetching = state.isFetchingSong,
                        onRemove = mediaOperations.onRemoveSong,
                        onEdit = { mediaOperations.onSongSheetToggle(true) },
                        isEditMode = state.isEditMode
                    )
                }
            }
            if (state.location != null) {
                item(key = "map_card", span = { GridItemSpan(2) }) {
                    JournalMapItem(
                        location = state.location!!,
                        onMapClick = { mediaOperations.onLocationDialogToggle(true) },
                        onRemove = mediaOperations.onRemoveLocation,
                        isEditMode = state.isEditMode
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

    if (showSongSheet) {
        AddSongSheet(
            songDetails = state.songDetails,
            isFetching = state.isFetchingSong,
            onFetchDetails = { link ->
                viewModel.onAction(JournalAction.FetchSong(link))
            },
            onRemoveSong = {
                viewModel.onAction(JournalAction.RemoveSong)
            },
            onDismiss = { showSongSheet = false }
        )
    }
    if (showLocationDialog) {
        AddLocationDialog(
            existingLocation = state.location,
            isEditMode = state.isEditMode,
            onLocationSelected = { loc ->
                viewModel.onAction(JournalAction.SetLocation(loc))
            },
            onDismiss = { showLocationDialog = false }
        )
    }
}