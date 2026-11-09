package com.denser.june.presentation.screens.home.timeline.components.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.media.storage.SongMediaStorage
import com.denser.june.presentation.components.RestrictedAsyncImage
import com.denser.june.presentation.components.SmallPlayPauseButton
import com.denser.june.presentation.theme.LocalInternetAllowed
import com.denser.june.presentation.theme.rememberDynamicThemeColors

@Composable
fun DockedMusicPlayer(
    song: SongDetails,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInternetAllowed = LocalInternetAllowed.current
    val localArtFile = remember(song.localThumbnailPath) {
        SongMediaStorage.resolveSongMedia(context, song.localThumbnailPath, "art")
    }
    val artModel = remember(localArtFile, song.thumbnailUrl, isInternetAllowed) {
        if (localArtFile != null) localArtFile else if (isInternetAllowed) song.thumbnailUrl else null
    }
    val themeColors = rememberDynamicThemeColors(artModel)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = themeColors.surface,
        contentColor = themeColors.onSurface,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    RestrictedAsyncImage(
                        imageUrl = song.thumbnailUrl,
                        localPath = song.localThumbnailPath,
                        iconSize = 20.dp,
                        iconTint = themeColors.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = themeColors.onSurface
                    )
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = themeColors.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                SmallPlayPauseButton(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    enabled = true,
                    onClick = onPlayPause,
                    containerColor = themeColors.primaryContainer,
                    contentColor = themeColors.onPrimaryContainer
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 16.dp),
                color = themeColors.primaryContainer,
                trackColor = themeColors.primaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}
