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
    enablePlayback: Boolean = true,
    operations: MediaOperations,
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(24.dp)
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
                    )
                }
                2 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        operations = operations,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    JournalMediaItem(
                        path = mediaList[1],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isLargeItem = false,
                        enablePlayback = enablePlayback,
                        operations = operations,
                    )
                }
                3 -> {
                    JournalMediaItem(
                        path = mediaList[0],
                        modifier = Modifier.weight(0.66f).fillMaxHeight(),
                        isLargeItem = true,
                        enablePlayback = enablePlayback,
                        operations = operations,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(0.34f).fillMaxHeight()) {
                        JournalMediaItem(
                            path = mediaList[1],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
                            operations = operations,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        JournalMediaItem(
                            path = mediaList[2],
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            isLargeItem = false,
                            enablePlayback = enablePlayback,
                            operations = operations,
                        )
                    }
                }
            }
        }
    }
}