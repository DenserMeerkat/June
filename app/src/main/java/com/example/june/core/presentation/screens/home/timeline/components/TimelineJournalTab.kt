package com.example.june.core.presentation.screens.home.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.june.R
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.domain.utils.toDayOfMonth
import com.example.june.core.domain.utils.toShortMonth
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import org.koin.compose.koinInject

@Composable
fun TimelineJournalTab(
    journals: List<Journal>,
    bottomPadding: Dp
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
                        navigator.navigateTo(Route.Journal(journal.id))
                    }
                )
            }
        }
    }
}

@Composable
fun TimelineJournalTile(
    journal: Journal,
    onClick: () -> Unit
) {
    val wordCount = remember(journal.content) {
        if (journal.content.isBlank()) 0
        else journal.content.trim().split("\\s+".toRegex()).size
    }
    val hasMedia = remember(journal.images) { journal.images.isNotEmpty() }
    val hasMusic = remember(journal.songDetails) { journal.songDetails != null }
    val hasLocation = remember(journal.location) { journal.location != null }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
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
            Spacer(modifier = Modifier.width(4.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val titleText = journal.title.ifBlank { "Untitled Entry" }
                val contentText = journal.content.ifBlank { "No content" }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contentText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasMedia) {
                            Icon(
                                painter = painterResource(R.drawable.photo_24px),
                                contentDescription = "Media",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (hasMusic) {
                            Icon(
                                painter = painterResource(R.drawable.music_note_24px),
                                contentDescription = "Music",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (hasLocation) {
                            Icon(
                                painter = painterResource(R.drawable.location_on_24px),
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}