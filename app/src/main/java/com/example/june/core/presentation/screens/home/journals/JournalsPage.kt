package com.example.june.core.presentation.screens.home.journals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.core.presentation.screens.home.journals.components.JournalCard
import com.example.june.viewmodels.HomeJournalVM
import org.koin.compose.viewmodel.koinViewModel

import com.example.june.R
import com.example.june.core.presentation.screens.home.journals.components.RecentJournalCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalsPage() {
    val viewModel: HomeJournalVM = koinViewModel()
    val journals by viewModel.journals.collectAsStateWithLifecycle()

    val draftJournals =
        remember(journals) { journals.filter { it.isDraft }.sortedByDescending { it.dateTime } }
    val bookmarkedJournals = remember(journals) { journals.filter { it.isBookmarked } }

    val nonDrafts = remember(journals) {
        journals
            .filter { !it.isDraft }
            .sortedByDescending { it.dateTime }
    }

    val recentJournal = remember(nonDrafts) { nonDrafts.firstOrNull() }
    val moreJournals = remember(nonDrafts) { nonDrafts.drop(1) }


    if (journals.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.auto_stories_off_24px),
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
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                if (recentJournal != null) {
                    item(key = "header_recent") {
                        SectionHeader(
                            title = "Recent",
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateItem()
                        )
                    }
                    item(key = "recent_${recentJournal.id}") {
                        RecentJournalCard(
                            journal = recentJournal,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                if (draftJournals.isNotEmpty()) {
                    item(key = "header_drafts") {
                        SectionHeader(
                            title = "Drafts",
                            modifier = Modifier.animateItem()
                        )
                    }
                    items(draftJournals, key = { "draft_${it.id}" }) { journal ->
                        JournalCard(
                            journal = journal,
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
                        JournalCard(
                            journal = journal,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                if (moreJournals.isNotEmpty()) {
                    item(key = "header_more") {
                        SectionHeader(
                            title = "More entries",
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .animateItem()
                        )
                    }
                    items(moreJournals, key = { "more_${it.id}" }) { journal ->
                        JournalCard(
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
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.padding(vertical = 4.dp, horizontal = 16.dp)
    )
}