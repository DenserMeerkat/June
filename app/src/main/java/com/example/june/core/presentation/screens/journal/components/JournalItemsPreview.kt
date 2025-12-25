package com.example.june.core.presentation.screens.journal.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.ImageLoader

@Composable
fun JournalItemsPreview(
    mediaPaths: List<String>,
    isEditMode: Boolean,
    imageLoader: ImageLoader,
    onRemoveMedia: (String) -> Unit,
    onMoveToFront: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemAtDataFront = mediaPaths.lastOrNull()

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
                    JournalItem(
                        path = mediaChunk[0],
                        modifier = Modifier.fillMaxSize(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        isLargeItem = true,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[0])
                    )
                }

                2 -> {
                    JournalItem(
                        path = mediaChunk[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        isLargeItem = true,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    JournalItem(
                        path = mediaChunk[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        isLargeItem = true,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[1])
                    )
                }

                3 -> {
                    JournalItem(
                        path = mediaChunk[0],
                        modifier = Modifier
                            .weight(0.66f)
                            .fillMaxHeight(),
                        isEditMode = isEditMode,
                        imageLoader = imageLoader,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        isLargeItem = true,
                        showMoveToFront = shouldShowMoveOption(mediaChunk[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxHeight()
                    ) {
                        JournalItem(
                            path = mediaChunk[1],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            isEditMode = isEditMode,
                            imageLoader = imageLoader,
                            onRemove = onRemoveMedia,
                            onMoveToFront = onMoveToFront,
                            isLargeItem = false,
                            showMoveToFront = shouldShowMoveOption(mediaChunk[1])
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        JournalItem(
                            path = mediaChunk[2],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            isEditMode = isEditMode,
                            imageLoader = imageLoader,
                            onRemove = onRemoveMedia,
                            onMoveToFront = onMoveToFront,
                            isLargeItem = false,
                            showMoveToFront = shouldShowMoveOption(mediaChunk[2])
                        )
                    }
                }
            }
        }
    }
}