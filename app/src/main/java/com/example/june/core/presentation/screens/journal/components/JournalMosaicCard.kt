package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun JournalMosaicCard(
    modifier: Modifier = Modifier,
    mediaList: List<String>,
    isEditMode: Boolean,
    enablePlayback: Boolean = true,
    onRemoveMedia: (String) -> Unit = {},
    onMoveToFront: (String) -> Unit = {},
    itemAtDataFront: String? = null,
    onMediaClick: ((String) -> Unit)? = null,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(24.dp)
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(roundedCornerShape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            fun shouldShowMoveOption(path: String) = path != itemAtDataFront

            val getOnClick: (String) -> (() -> Unit)? = { path ->
                if (onMediaClick != null) { { onMediaClick(path) } } else null
            }

            when (mediaList.size) {
                1 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.fillMaxSize(),
                        isEditMode = isEditMode,
                        isLargeItem = true,
                        enablePlayback = enablePlayback,
                        onTap = getOnClick(mediaList[0]),
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[0])
                    )
                }
                2 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        onTap = getOnClick(mediaList[0]),
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    JournalMediaItem(
                        path = mediaList[1],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        onTap = getOnClick(mediaList[1]),
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[1])
                    )
                }
                3 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(0.66f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        isLargeItem = true,
                        enablePlayback = enablePlayback,
                        onTap = getOnClick(mediaList[0]),
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(0.34f).fillMaxHeight()) {
                        JournalMediaItem(
                            path = mediaList[1],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isEditMode = isEditMode,
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
                            onTap = getOnClick(mediaList[1]),
                            onRemove = onRemoveMedia,
                            onMoveToFront = onMoveToFront,
                            showMoveToFront = shouldShowMoveOption(mediaList[1])
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        JournalMediaItem(
                            path = mediaList[2],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isEditMode = isEditMode,
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
                            onTap = getOnClick(mediaList[2]),
                            onRemove = onRemoveMedia,
                            onMoveToFront = onMoveToFront,
                            showMoveToFront = shouldShowMoveOption(mediaList[2])
                        )
                    }
                }
            }
        }
    }
}