package com.example.june.core.presentation.screens.journal.journalmedia

import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.june.core.presentation.screens.journal.components.JournalMediaItem
import com.example.june.core.presentation.screens.journal.components.MediaOperations
import com.example.june.viewmodels.JournalVM
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalMediaGalleryScreen(
    viewModel: JournalVM
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()

    val mediaOperations = remember(state.isEditMode, state.images) {
        MediaOperations(
            onRemove = { viewModel.onAction(JournalAction.RemoveImage(it)) },
            onMoveToFront = { viewModel.onAction(JournalAction.MoveImageToFront(it)) },
            onMediaClick = { path ->
                navigator.navigateTo(
                    Route.JournalMediaDetail(
                        journalId = state.journalId ?: 0L,
                        initialIndex = state.images.reversed().indexOf(path)
                    )
                )
            },
            isEditMode = state.isEditMode,
            frontMediaPath = state.images.lastOrNull()
        )
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = { Text("Media Gallery") },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75F)
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
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.images.reversed()) { index, path ->
                JournalMediaItem(
                    path = path,
                    modifier = Modifier.aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                    operations = mediaOperations,
                    isLargeItem = false,
                    enablePlayback = false,
                )
            }
        }
    }
}