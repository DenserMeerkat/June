package com.denser.june.presentation.screens.home.timeline.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.components.JournalActionButton
import com.denser.june.presentation.components.JournalCardColors
import com.denser.june.presentation.components.JournalMetadataBadges
import com.denser.june.presentation.components.rememberJournalCardColors
import com.denser.june.presentation.components.rememberJournalDisplayTitle
import com.denser.june.presentation.components.rememberJournalGroupShape
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import org.koin.compose.koinInject
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineJournalCard(
    journal: Journal,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    is24Hour: Boolean = false,
    isHighlighted: Boolean = false,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null,
    onJournalClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    colors: JournalCardColors = rememberJournalCardColors(journal)
) {
    val navigator = if (onJournalClick == null) koinInject<AppNavigator>() else null
    val displayTitle = rememberJournalDisplayTitle(journal)

    val highlightBorderColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 600),
        label = "journal_card_highlight"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = highlightBorderColor, shape = shape)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            TimelineDateColumn(
                dateTime = journal.dateTime,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Top),
                verticalArrangement = Arrangement.Top
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
    }
}

@Composable
fun TimelineDayGroup(
    modifier: Modifier = Modifier,
    date: LocalDate,
    journals: List<Journal>,
    is24Hour: Boolean,
    isHighlighted: Boolean = false,
    onToggleBookmark: (String) -> Unit,
    onJournalClick: ((Journal) -> Unit)? = null,
    onLongClick: (Journal) -> Unit,
) {
    val highlightBorderColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 600),
        label = "day_group_highlight"
    )

    val groupShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = highlightBorderColor, shape = groupShape),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        journals.forEachIndexed { index, journal ->
            val shape = rememberJournalGroupShape(index = index, totalCount = journals.size)

            TimelineJournalCard(
                journal = journal,
                shape = shape,
                is24Hour = is24Hour,
                onToggleBookmark = { onToggleBookmark(journal.id) },
                onJournalClick = onJournalClick?.let { { it(journal) } },
                onLongClick = { onLongClick(journal) }
            )
        }
    }
}
