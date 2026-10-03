package com.denser.june.presentation.screens.editor.components

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
    enablePlayback: Boolean = false, // TODO: Resolve concurrent playback crash
    operations: MediaOperations,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(24.dp),
    processingMediaPaths: Set<String> = emptySet(),
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(roundedCornerShape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            when (mediaList.size) {
                1 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.fillMaxSize(),
                        isLargeItem = true,
                        enablePlayback = enablePlayback,
                        operations = operations,
                        isProcessing = processingMediaPaths.contains(mediaList[0]),
                    )
                }
                2 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        operations = operations,
                        isProcessing = processingMediaPaths.contains(mediaList[0]),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    JournalMediaItem(
                        path = mediaList[1],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        operations = operations,
                        isProcessing = processingMediaPaths.contains(mediaList[1]),
                    )
                }
                3 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(0.66f).fillMaxHeight(),
                        isLargeItem = true,
                        enablePlayback = enablePlayback,
                        operations = operations,
                        isProcessing = processingMediaPaths.contains(mediaList[0]),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(0.34f).fillMaxHeight()) {
                        JournalMediaItem(
                            path = mediaList[1],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
                            operations = operations,
                            isProcessing = processingMediaPaths.contains(mediaList[1]),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        JournalMediaItem(
                            path = mediaList[2],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
                            operations = operations,
                            isProcessing = processingMediaPaths.contains(mediaList[2]),
                        )
                    }
                }
            }
        }
    }
}