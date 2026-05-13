package com.denser.june.presentation.screens.home.components

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
import androidx.compose.ui.graphics.Color
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toFullDate
import com.denser.june.core.utils.toShortMonth
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.editor.components.JournalMosaicCard
import com.denser.june.presentation.screens.editor.components.MediaOperations
import com.denser.june.presentation.screens.home.journals.JournalsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import com.denser.june.core.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalCard(
    journal: Journal,
    modifier: Modifier,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val viewModel: JournalsVM = koinViewModel()
    val navigator = koinInject<AppNavigator>()

    val mediaOperations = MediaOperations(onMediaClick = null)

    val semanticColor = when (journal.aiColorWeight?.lowercase()) {
        "red" -> MaterialTheme.colorScheme.errorContainer
        "blue", "cyan" -> MaterialTheme.colorScheme.primaryContainer
        "green" -> MaterialTheme.colorScheme.tertiaryContainer
        "purple", "magenta" -> MaterialTheme.colorScheme.secondaryContainer
        "yellow", "orange" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp) // Slightly taller for more breathing room
            .clip(RoundedCornerShape(32.dp)) // Expressive rounded corners
            .combinedClickable(
                onClick = { navigator.navigateTo(Route.Editor(journal.id), isSingleTop = true) },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = semanticColor
        ),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Soft elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(24.dp), // Softer inner corners
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (journal.images.isNotEmpty()) {
                    JournalMosaicCard(
                        mediaList = listOf(journal.images.last()),
                        enablePlayback = false,
                        modifier = Modifier.fillMaxSize(),
                        operations = mediaOperations,
                        roundedCornerShape = RoundedCornerShape(24.dp)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.book_5_24px), // Replace with journal icon if needed
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, // Pop of color
                            modifier = Modifier.size(32.dp)
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
                    text = journal.title.ifBlank { journal.content.ifBlank { "Untitled Entry" } },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = journal.dateTime.toFullDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Subtle action button
            FilledIconButton(
                onClick = { onActionClick?.invoke() ?: viewModel.toggleBookmark(journal.id) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent, // Make it blend in more
                    contentColor = if (journal.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(48.dp) // Larger touch target
            ) {
                Icon(
                    painter = painterResource(
                        actionIcon ?: if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill 
                        else R.drawable.bookmark_24px
                    ),
                    contentDescription = if (actionIcon != null) "Action" else "Toggle Bookmark",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentJournalCard(
    journal: Journal,
    modifier: Modifier,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    if (journal.images.isEmpty()) {
        JournalCard(
            journal = journal,
            modifier = modifier,
            actionIcon = actionIcon,
            onActionClick = onActionClick,
            onLongClick = onLongClick
        )
    } else {
        val viewModel: JournalsVM = koinViewModel()
        val navigator = koinInject<AppNavigator>()

        val displayImages = remember(journal.images) {
            journal.images.reversed().take(3)
        }

        val mediaOperations = MediaOperations(onMediaClick = null)

        val semanticColor = when (journal.aiColorWeight?.lowercase()) {
            "red" -> MaterialTheme.colorScheme.errorContainer
            "blue", "cyan" -> MaterialTheme.colorScheme.primaryContainer
            "green" -> MaterialTheme.colorScheme.tertiaryContainer
            "purple", "magenta" -> MaterialTheme.colorScheme.secondaryContainer
            "yellow", "orange" -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .combinedClickable(
                    onClick = {
                        navigator.navigateTo(
                            Route.Editor(journal.id),
                            isSingleTop = true
                        )
                    },
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = semanticColor
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = journal.dateTime.toDayOfMonth(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = journal.dateTime.toShortMonth(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = journal.title.ifBlank { journal.content.ifBlank { "Untitled" } },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Journal Entry", // Added a subtype text for better hierarchy
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledIconButton(
                        onClick = { onActionClick?.invoke() ?: viewModel.toggleBookmark(journal.id) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (journal.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                actionIcon ?: if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill 
                                else R.drawable.bookmark_24px
                            ),
                            contentDescription = if (actionIcon != null) "Action" else "Toggle Bookmark",
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                JournalMosaicCard(
                    mediaList = displayImages,
                    enablePlayback = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp), // Taller images
                    operations = mediaOperations,
                    roundedCornerShape = RoundedCornerShape(24.dp) // Softer image corners
                )
            }
        }
    }
}
