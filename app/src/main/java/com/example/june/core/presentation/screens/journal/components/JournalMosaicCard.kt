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
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(24.dp)
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(roundedCornerShape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            fun shouldShowMoveOption(path: String) = path != itemAtDataFront

            when (mediaList.size) {
                1 -> {
                    JournalItem(
                        path = mediaList[0],
                        modifier = Modifier.fillMaxSize(),
                        isEditMode = isEditMode,
                        isLargeItem = true,
                        enablePlayback = enablePlayback,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[0])
                    )
                }
                2 -> {
                    JournalItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    JournalItem(
                        path = mediaList[1],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[1])
                    )
                }
                3 -> {
                    JournalItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(0.66f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        isLargeItem = true,
                        enablePlayback = enablePlayback,
                        onRemove = onRemoveMedia,
                        onMoveToFront = onMoveToFront,
                        showMoveToFront = shouldShowMoveOption(mediaList[0])
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(0.34f).fillMaxHeight()) {
                        JournalItem(
                            path = mediaList[1],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isEditMode = isEditMode,
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
                            onRemove = onRemoveMedia,
                            onMoveToFront = onMoveToFront,
                            showMoveToFront = shouldShowMoveOption(mediaList[1])
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        JournalItem(
                            path = mediaList[2],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isEditMode = isEditMode,
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
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