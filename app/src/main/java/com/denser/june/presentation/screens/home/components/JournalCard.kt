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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toFullDate
import com.denser.june.core.utils.toShortMonth
import com.denser.june.presentation.components.JournalActionButton
import com.denser.june.presentation.components.JournalCardColors
import com.denser.june.presentation.components.JournalMetadataBadges
import com.denser.june.presentation.components.JournalThumbnail
import com.denser.june.presentation.components.JuneBadge
import com.denser.june.presentation.components.rememberJournalCardColors
import com.denser.june.presentation.components.rememberJournalDisplayTitle
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.editor.components.JournalMosaicCard
import com.denser.june.presentation.screens.editor.components.MediaOperations
import org.koin.compose.koinInject
import com.denser.june.core.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalCard(
    journal: Journal,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    is24Hour: Boolean = false,
    showDate: Boolean = false,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null,
    onJournalClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    colors: JournalCardColors = rememberJournalCardColors(journal)
) {
    val navigator = if (onJournalClick == null) koinInject<AppNavigator>() else null
    val mediaOperations = remember { MediaOperations(onMediaClick = null) }
    val displayTitle = rememberJournalDisplayTitle(journal)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = {
                    if (onJournalClick != null) {
                        onJournalClick()
                    } else {
                        navigator?.navigateTo(Route.Editor(journal.id), isSingleTop = true)
                    }
                },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = colors.containerColor
        ),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JournalThumbnail(
                journal = journal,
                containerColor = colors.innerContainerColor,
                mediaOperations = mediaOperations
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = journal.dateTime.toFullDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.dateColor,
                        fontWeight = FontWeight.SemiBold
                    )

                    JuneBadge(
                        show = journal.isDraft,
                        icon = R.drawable.edit_24px_fill
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            JournalActionButton(
                isBookmarked = journal.isBookmarked,
                innerContainerColor = colors.innerContainerColor,
                actionIcon = actionIcon,
                onActionClick = onActionClick,
                onToggleBookmark = onToggleBookmark
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentJournalCard(
    journal: Journal,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    is24Hour: Boolean = false,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null,
    onJournalClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    colors: JournalCardColors = rememberJournalCardColors(journal)
) {
    if (journal.images.isEmpty()) {
        JournalCard(
            journal = journal,
            modifier = modifier,
            shape = shape,
            is24Hour = is24Hour,
            actionIcon = actionIcon,
            onActionClick = onActionClick,
            onToggleBookmark = onToggleBookmark,
            onJournalClick = onJournalClick,
            onLongClick = onLongClick,
            colors = colors
        )
    } else {
        val navigator = if (onJournalClick == null) koinInject<AppNavigator>() else null
        val displayImages = remember(journal.images) {
            journal.images.reversed().take(3)
        }
        val mediaOperations = remember { MediaOperations(onMediaClick = null) }
        val displayTitle = rememberJournalDisplayTitle(journal)

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                onClick = {
                    if (onJournalClick != null) {
                        onJournalClick()
                    } else {
                        navigator?.navigateTo(
                            Route.Editor(journal.id),
                            isSingleTop = true
                        )
                    }
                },
                onLongClick = onLongClick
            ),
            colors = CardDefaults.cardColors(
                containerColor = colors.containerColor
            ),
            shape = shape
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
                            text = journal.dateTime.toDayOfMonth(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = journal.dateTime.toShortMonth(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.dateColor,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        JournalMetadataBadges(journal = journal)
                    }
                    JournalActionButton(
                        isBookmarked = journal.isBookmarked,
                        innerContainerColor = colors.innerContainerColor,
                        actionIcon = actionIcon,
                        onActionClick = onActionClick,
                        onToggleBookmark = onToggleBookmark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                JournalMosaicCard(
                    mediaList = displayImages,
                    enablePlayback = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.7f),
                    operations = mediaOperations,
                    roundedCornerShape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}