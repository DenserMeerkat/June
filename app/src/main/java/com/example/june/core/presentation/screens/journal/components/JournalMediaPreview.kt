package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.june.R
import java.io.File

@Composable
fun JournalMediaPreview(
    mediaPaths: List<String>,
    isEditMode: Boolean,
    imageLoader: ImageLoader,
    onRemoveMedia: (String) -> Unit,
    onMoveToFront: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemAtDataFront = mediaPaths.last()

    val chunks = remember(mediaPaths) {
        mediaPaths.reversed().chunked(3)
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(chunks) { chunk ->
            val widthFraction = if (chunks.size == 1) 1f else 0.9f

            CarouselCard(
                mediaChunk = chunk,
                isEditMode = isEditMode,
                imageLoader = imageLoader,
                onRemoveMedia = onRemoveMedia,
                onMoveToFront = onMoveToFront,
                itemAtDataFront = itemAtDataFront,
                modifier = Modifier.fillParentMaxWidth(widthFraction)
            )
        }
    }
}

@Composable
private fun CarouselCard(
    mediaChunk: List<String>,
    isEditMode: Boolean,
    imageLoader: ImageLoader,
    onRemoveMedia: (String) -> Unit,
    onMoveToFront: (String) -> Unit,
    itemAtDataFront: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            fun shouldShowMoveOption(path: String): Boolean {
                return path != itemAtDataFront
            }

            when (mediaChunk.size) {
                1 -> {
                    MediaItem(
                        path = mediaChunk[0],
                        modifier = Modifier.fillMaxSize(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[0])
                    )
                }

                2 -> {
                    MediaItem(
                        path = mediaChunk[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    MediaItem(
                        path = mediaChunk[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[1])
                    )
                }

                3 -> {
                    MediaItem(
                        path = mediaChunk[0],
                        modifier = Modifier
                            .weight(0.66f)
                            .fillMaxHeight(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxHeight()
                    ) {
                        MediaItem(
                            path = mediaChunk[1],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            isEditMode = isEditMode,
                            imageLoader = imageLoader,
                            onRemove = onRemoveMedia,
                            onMoveToFront = onMoveToFront,
                            showMoveToFront = shouldShowMoveOption(mediaChunk[1])
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MediaItem(
                            path = mediaChunk[2],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            isEditMode = isEditMode,
                            imageLoader = imageLoader,
                            onRemove = onRemoveMedia,
                            onMoveToFront = onMoveToFront,
                            showMoveToFront = shouldShowMoveOption(mediaChunk[2])
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaItem(
    path: String,
    modifier: Modifier,
    isEditMode: Boolean,
    imageLoader: ImageLoader,
    onRemove: (String) -> Unit,
    onMoveToFront: (String) -> Unit,
    showMoveToFront: Boolean
) {
    val density = LocalDensity.current
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .onSizeChanged {
                itemHeight = with(density) { it.height.toDp() }
            }
            .clip(RoundedCornerShape(4.dp))
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            showMenu = true
                            pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                        },
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            tryAwaitRelease()
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    )
                }
            }
    ) {
        AsyncImage(
            model = File(path),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        if (path.endsWith("mp4", ignoreCase = true)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.play_circle_24px),
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 3.dp,
            modifier = Modifier.padding(horizontal = 8.dp),
            offset = pressOffset.copy(y = pressOffset.y - itemHeight)
        ) {
            DropdownMenuItem(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                text = { Text("Delete") },
                onClick = {
                    onRemove(path)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = null
                    )
                }
            )

            if (showMoveToFront) {
                DropdownMenuItem(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    text = { Text("Move to the Front") },
                    onClick = {
                        onMoveToFront(path)
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.turn_left_24px),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}