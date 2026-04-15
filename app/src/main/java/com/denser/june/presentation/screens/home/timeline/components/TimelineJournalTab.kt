package com.denser.june.presentation.screens.home.timeline.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.JuneBadge
import org.koin.compose.koinInject

import com.denser.june.core.R
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toShortMonth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineJournalTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    onLongClick: ((Journal) -> Unit)? = null
) {
    val navigator = koinInject<AppNavigator>()

    if (journals.isEmpty()) {
        EmptyStateMessage("No journals this month.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(journals, key = { it.id }) { journal ->
                TimelineJournalTile(
                    journal = journal,
                    onClick = {
                        navigator.navigateTo(Route.Editor(journal.id), isSingleTop = true)
                    },
                    onLongClick = onLongClick?.let { cb -> { cb(journal) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineJournalTile(
    journal: Journal,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val wordCount = remember(journal.content) {
        val trimmed = journal.content.trim()
        if (trimmed.isEmpty()) 0
        else trimmed.split("\\s+".toRegex()).size
    }
    val mediaCount = remember(journal.images) { journal.images.size }
    val hasMusic = remember(journal.songDetails) { journal.songDetails != null }
    val hasLocation = remember(journal.location) { journal.location != null }
    val tagCount = remember(journal.tags) { journal.tags.size }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelineDateColumn(dateTime = journal.dateTime)
            Spacer(modifier = Modifier.width(2.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val titleText = journal.title.ifBlank { "Untitled" }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JuneBadge(
                        show = true,
                        icon = if (journal.cloudId != null) R.drawable.cloud_24px else R.drawable.devices_24px,
                        label = if (journal.cloudId != null) "Cloud" else "Local"
                    )
                    JuneBadge(
                        show = wordCount > 0,
                        icon = R.drawable.article_24px,
                        label = "$wordCount ${if (wordCount == 1) "word" else "words"}"
                    )
                    JuneBadge(
                        show = mediaCount > 0,
                        icon = R.drawable.photo_24px,
                        label = if (mediaCount > 1) "$mediaCount" else null
                    )
                    JuneBadge(
                        show = hasMusic,
                        icon = R.drawable.music_note_24px
                    )
                    JuneBadge(
                        show = hasLocation,
                        icon = R.drawable.location_on_24px
                    )
                    JuneBadge(
                        show = tagCount > 0,
                        icon = R.drawable.sell_24px,
                        label = "$tagCount"
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                val emoji = journal.emoji
                if (emoji != null) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}