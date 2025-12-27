package com.example.june.core.presentation.screens.home.journals.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.example.june.R
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.domain.utils.toDayOfMonth
import com.example.june.core.domain.utils.toFullDate
import com.example.june.core.domain.utils.toShortMonth
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.components.JuneIconButton
import com.example.june.core.presentation.screens.journal.components.JournalMosaicCard
import com.example.june.core.presentation.screens.journal.components.MediaOperations
import com.example.june.viewmodels.HomeJournalVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalCard(
    journal: Journal,
    modifier: Modifier
) {
    val viewModel: HomeJournalVM = koinViewModel()
    val navigator = koinInject<AppNavigator>()

    val mediaOperations = MediaOperations(
        onRemove = { },
        onMoveToFront = { },
        onMediaClick = null,
        isEditMode = false,
        frontMediaPath = null
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = { navigator.navigateTo(Route.Journal(journal.id), isSingleTop = true) },
                onLongClick = {}
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(96.dp, 60.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6F),
            ) {
                if (journal.images.isNotEmpty()) {
                    JournalMosaicCard(
                        mediaList = listOf(journal.images.last()),
                        enablePlayback = false,
                        modifier = Modifier.fillMaxSize(),
                        operations = mediaOperations,
                        roundedCornerShape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.book_5_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = journal.dateTime.toFullDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8F)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = journal.title.ifBlank { journal.content.ifBlank { "Add title" } },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            JuneIconButton(
                onClick = { viewModel.toggleBookmark(journal.id) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                icon = if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill else R.drawable.bookmark_24px,
                contentDescription = "Toggle Bookmark",
                iconSize = 20.dp
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentJournalCard(
    journal: Journal,
    modifier: Modifier
) {
    if (journal.images.isEmpty()) {
        JournalCard(
            journal = journal,
            modifier = modifier
        )
    } else {
        val viewModel: HomeJournalVM = koinViewModel()
        val navigator = koinInject<AppNavigator>()

        val displayImages = remember(journal.images) {
            journal.images.reversed().take(3)
        }

        val mediaOperations = MediaOperations(
            onRemove = { },
            onMoveToFront = { },
            onMediaClick = null,
            isEditMode = false,
            frontMediaPath = null
        )

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = {
                        navigator.navigateTo(
                            Route.Journal(journal.id),
                            isSingleTop = true
                        )
                    },
                    onLongClick = {}
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = journal.dateTime.toShortMonth(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8F)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = journal.dateTime.toDayOfMonth(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        modifier = Modifier.weight(1f),
                        text = journal.title.ifBlank { journal.content.ifBlank { "Untitled" } },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    JuneIconButton(
                        onClick = { viewModel.toggleBookmark(journal.id) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        icon = if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill else R.drawable.bookmark_24px,
                        contentDescription = "Toggle Bookmark",
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                JournalMosaicCard(
                    mediaList = displayImages,
                    enablePlayback = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    operations = mediaOperations,
                    roundedCornerShape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}