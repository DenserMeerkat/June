package com.example.june.core.presentation.screens.home.journals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.presentation.screens.home.journals.components.JournalItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalsPage(
    journals: List<Journal>
) {
    val sortedJournals = remember(journals) { journals.sortedByDescending { it.dateTime } }
    val recentJournal = remember(sortedJournals) { sortedJournals.firstOrNull() }
    val pastJournals = remember(sortedJournals) { sortedJournals.drop(1) }
    val bookmarkedJournals = remember(journals) { journals.filter { it.isBookmarked } }

    if (journals.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Note,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No journals yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentPadding = PaddingValues(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (recentJournal != null) {
                    item(key = "header_recent") { 
                        SectionHeader(
                            title = "Recent",
                            modifier = Modifier.animateItem() 
                        )
                    }
                    item(key = "recent_${recentJournal.id}") {
                        JournalItem(
                            journal = recentJournal,
                            modifier = Modifier.animateItem() 
                        )
                    }
                }

                if (bookmarkedJournals.isNotEmpty()) {
                    item(key = "header_bookmarks") {
                        SectionHeader(
                            title = "Bookmarks",
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateItem() 
                        )
                    }
                    items(bookmarkedJournals, key = { "bm_${it.id}" }) { journal ->
                        JournalItem(
                            journal = journal,
                            modifier = Modifier.animateItem() 
                        )
                    }
                }

                if (pastJournals.isNotEmpty()) {
                    item(key = "header_past") {
                        SectionHeader(
                            title = "Past entries",
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateItem() 
                        )
                    }
                    items(pastJournals, key = { "past_${it.id}" }) { journal ->
                        JournalItem(
                            journal = journal,
                            modifier = Modifier.animateItem() 
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}