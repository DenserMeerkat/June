package com.denser.june.presentation.screens.home.timeline.components.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.screens.home.timeline.TimelineTab
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.screens.home.timeline.components.audio.TimelineAudioTab
import com.denser.june.presentation.screens.home.timeline.components.journal.TimelineJournalTab
import com.denser.june.presentation.screens.home.timeline.components.map.TimelineMapTab
import com.denser.june.presentation.screens.home.timeline.components.media.TimelineMediaTab
import com.denser.june.presentation.screens.home.timeline.components.music.TimelineMusicTab
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimelineTabs(
    selectedTab: TimelineTab,
    journals: List<Journal>,
    onTabSelected: (TimelineTab) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    is24Hour: Boolean = false,
    targetScrollDate: LocalDate? = null,
    onScrollConsumed: (() -> Unit)? = null,
    onToggleBookmark: ((String) -> Unit)? = null,
    onLongClickJournal: ((Journal) -> Unit)? = null
) {
    val interactionSources = remember { List(TimelineTab.entries.size) { MutableInteractionSource() } }

    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            ButtonGroup(
                overflowIndicator = {},
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(44.dp)
            ) {
                TimelineTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == tab
                    val interactionSource = interactionSources[index]
                    customItem(
                        buttonGroupContent = {
                            val shape = when {
                                TimelineTab.entries.size == 1 -> ToggleButtonDefaults.shapes()
                                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == TimelineTab.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }

                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { onTabSelected(tab) },
                                shapes = shape,
                                modifier = Modifier
                                    .weight(if (isSelected) 1.25f else 1f)
                                    .fillMaxHeight()
                                    .animateWidth(interactionSource),
                                contentPadding = PaddingValues(0.dp),
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                interactionSource = interactionSource
                            ) {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = stringResource(tab.labelRes),
                                    modifier = Modifier.size(if (isSelected) 22.dp else 20.dp)
                                )
                            }
                        },
                        menuContent = {}
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    TimelineTab.Journals -> TimelineJournalTab(
                        journals = journals,
                        bottomPadding = bottomPadding,
                        is24Hour = is24Hour,
                        targetScrollDate = targetScrollDate,
                        onScrollConsumed = onScrollConsumed,
                        onToggleBookmark = onToggleBookmark,
                        onLongClick = onLongClickJournal
                    )

                    TimelineTab.Media -> TimelineMediaTab(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )

                    TimelineTab.Audio -> TimelineAudioTab(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )

                    TimelineTab.Map -> TimelineMapTab(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )

                    TimelineTab.Music -> TimelineMusicTab(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    JunePlaceholderPage(
        modifier = Modifier.padding(bottom = 100.dp),
        subtitle = message
    )
}

