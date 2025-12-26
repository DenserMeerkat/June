package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JournalItemsPreview(
    mediaPaths: List<String>,
    isEditMode: Boolean,
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

            JournalMosaicCard(
                modifier = Modifier.fillParentMaxWidth(widthFraction),
                mediaList = chunk,
                isEditMode = isEditMode,
                onRemoveMedia = onRemoveMedia,
                onMoveToFront = onMoveToFront,
                itemAtDataFront = itemAtDataFront
            )
        }
    }
}
