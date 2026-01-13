package com.denser.june.core.presentation.screens.home.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.denser.june.core.domain.data_classes.Journal
import com.denser.june.core.domain.data_classes.SongDetails
import com.denser.june.core.domain.utils.toDayOfMonth
import com.denser.june.core.domain.utils.toShortMonth
import com.denser.june.core.presentation.components.ListenDropdownMenu
import com.denser.june.core.presentation.components.PlayPauseButton
import com.denser.june.core.presentation.utils.rememberDynamicThemeColors
import com.denser.june.viewmodels.TimelineVM
import org.koin.compose.viewmodel.koinViewModel

import com.denser.june.R

@Composable
fun TimelineMusicTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    viewModel: TimelineVM = koinViewModel()
) {
    val musicJournals = remember(journals) {
        journals.filter { it.songDetails != null }
    }

    val activeSong by viewModel.activeSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.sliderProgress.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }
    LaunchedEffect(musicJournals) {
        if (activeSong == null && musicJournals.isNotEmpty()) {
            viewModel.onSongSelected(
                musicJournals.first().songDetails!!,
                musicJournals.first().id,
                false
            )
            viewModel.pause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (musicJournals.isEmpty()) {
            EmptyStateMessage("No music tracked this month.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = bottomPadding + 16.dp + 80.dp,
                    top = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(musicJournals, key = { it.id }) { journal ->
                    val song = journal.songDetails!!
                    MusicListTile(
                        journal = journal,
                        song = song,
                        isActive = activeSong?.previewUrl == song.previewUrl,
                        onClick = { viewModel.onSongSelected(song, journal.id) }
                    )
                }
            }
        }

        if (activeSong != null) {
            DockedMiniPlayer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding),
                song = activeSong!!,
                isPlaying = isPlaying,
                isLoading = isLoading,
                progress = progress,
                onPlayPause = { viewModel.togglePlayPause() }
            )
        }
    }
}

@Composable
fun DockedMiniPlayer(
    song: SongDetails,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = rememberDynamicThemeColors(song.thumbnailUrl)

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
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
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
                PlayPauseButton(
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

@Composable
fun MusicListTile(
    journal: Journal,
    song: SongDetails,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val availableLinks = remember(song.links) {
        listOf(
            "Spotify" to song.links.spotify,
            "Apple Music" to song.links.appleMusic,
            "YouTube Music" to song.links.youtubeMusic,
            "YouTube" to song.links.youtube,
            "Deezer" to song.links.deezer,
            "SoundCloud" to song.links.soundcloud,
            "Tidal" to song.links.tidal,
            "Amazon Music" to song.links.amazonMusic
        ).filter { it.second != null }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = journal.dateTime.toDayOfMonth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = journal.dateTime.toShortMonth(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ListenDropdownMenu(
                availableLinks = availableLinks,
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                trigger = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert_24px),
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }
}