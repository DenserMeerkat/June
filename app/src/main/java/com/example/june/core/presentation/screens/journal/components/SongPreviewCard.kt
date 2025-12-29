package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.june.R
import com.example.june.core.domain.data_classes.SongDetails
import com.example.june.core.presentation.components.JuneSongPlayerCard
import com.example.june.core.presentation.utils.rememberSongPlayerState

@Composable
fun SongPreviewCard(
    details: SongDetails?,
    isFetching: Boolean,
    onRemove: () -> Unit
) {
    val playerState = rememberSongPlayerState(previewUrl = details?.previewUrl)

    when {
        isFetching -> {
            SongCardPlaceholder(isLoading = true)
        }

        details != null -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                JuneSongPlayerCard(
                    details = details,
                    isPlaying = playerState.isPlaying,
                    sliderValue = playerState.sliderValue,
                    onPlayPause = playerState.onPlayPause,
                    onSeek = playerState.onSeek,
                    onSeekFinished = playerState.onSeekFinished,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp, bottom = 36.dp)
                ) {
                    RemoveButton(onRemove)
                }
            }
        }

        else -> {
            SongCardPlaceholder(isLoading = false)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RemoveButton(
    onRemove: () -> Unit
) {
    FilledIconButton(
        onClick = onRemove,
        shape = IconButtonDefaults.largePressedShape,
        modifier = Modifier.size(56.dp).alpha(0.8F),
        colors = IconButtonDefaults.iconButtonColors().copy(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.delete_24px),
            contentDescription = "Remove Song",
            modifier = Modifier.size(32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongCardPlaceholder(
    isLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(64.dp),
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.music_note_2_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No song attached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}