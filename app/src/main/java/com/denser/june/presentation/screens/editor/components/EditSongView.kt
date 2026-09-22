package com.denser.june.presentation.screens.editor.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.utils.FileUtils
import com.denser.june.presentation.components.JuneTextField
import com.denser.june.presentation.components.RestrictedAsyncImage

import kotlinx.coroutines.launch
@Composable
fun EditSongView(
    modifier: Modifier = Modifier,
    song: SongDetails,
    editScope: EditSongScope = EditSongScope.Library,
    title: String,
    onTitleChange: (String) -> Unit,
    artist: String,
    onArtistChange: (String) -> Unit,
    album: String,
    onAlbumChange: (String) -> Unit,
    genre: String,
    onGenreChange: (String) -> Unit,
    artPath: String?,
    isArtRemoved: Boolean,
    onArtPathChange: (String) -> Unit,
    onRemoveArt: () -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val persistedPath = FileUtils.persistSongArt(context, uri)
            if (persistedPath != null) {
                onArtPathChange(persistedPath)
            }
        }
    }

    val hasArtwork = !isArtRemoved && (artPath != null || song.thumbnailUrl != null || song.localThumbnailPath != null)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EditScopeChip(scope = editScope)


        Box(
            modifier = Modifier
                .padding(end = 12.dp, bottom = 12.dp)
                .wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (!hasArtwork) {
                    Icon(
                        painter = painterResource(R.drawable.music_note_24px),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                } else {
                    RestrictedAsyncImage(
                        imageUrl = song.thumbnailUrl,
                        localPath = artPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            var showArtMenu by remember { mutableStateOf(false) }

            if (hasArtwork) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 10.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            if (enabled) {
                                showArtMenu = true
                            }
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert_24px),
                            contentDescription = "Artwork Options",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 180.dp)
                            .padding(horizontal = 8.dp),
                        expanded = showArtMenu,
                        onDismissRequest = { showArtMenu = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                        offset = DpOffset(x = 0.dp, y = 4.dp)
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            text = { Text("Change") },
                            onClick = {
                                showArtMenu = false
                                if (enabled) {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.edit_24px),
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            text = {
                                Text(
                                    text = "Remove",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showArtMenu = false
                                if (enabled) {
                                    onRemoveArt()
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            } else {
                SmallFloatingActionButton(
                    onClick = {
                        if (enabled) {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 10.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_photo_alternate_24px),
                        contentDescription = "Add Artwork",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        JuneTextField(
            value = title,
            onValueChange = onTitleChange,
            label = "Title",
            leadingIcon = R.drawable.music_note_24px,
            placeholder = "Song title",
            enabled = enabled
        )

        JuneTextField(
            value = artist,
            onValueChange = onArtistChange,
            label = "Artist",
            leadingIcon = R.drawable.person_24px,
            placeholder = "Artist name",
            enabled = enabled
        )

        JuneTextField(
            value = album,
            onValueChange = onAlbumChange,
            label = "Album",
            leadingIcon = R.drawable.art_track_24px,
            placeholder = "Album name",
            enabled = enabled
        )

        JuneTextField(
            value = genre,
            onValueChange = onGenreChange,
            label = "Genre",
            leadingIcon = R.drawable.category_24px,
            placeholder = "Genre",
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(96.dp))
    }
}
