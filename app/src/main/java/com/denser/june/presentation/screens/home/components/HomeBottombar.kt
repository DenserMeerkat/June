package com.denser.june.presentation.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.TagCategory
import com.denser.june.presentation.screens.home.HomeTab
import com.denser.june.presentation.utils.TagUtils
import kotlinx.coroutines.launch

private data class HomeFabConfig(
    val badgeIconRes: Int?,
    val badgeColor: Color,
    val containerColor: Color,
    val contentColor: Color,
    val contentDescription: String,
)

@Composable
private fun rememberHomeFabConfig(
    currentTab: HomeTab,
    selectedCategory: TagCategory,
    activeTag: String?
): HomeFabConfig {
    val activeTagCategory = if (currentTab == HomeTab.Tags && activeTag != null) {
        TagUtils.getCategoryForTag(activeTag)
    } else if (currentTab == HomeTab.Tags) {
        selectedCategory
    } else {
        null
    }

    val (containerColor, contentColor, badgeColor) = if (activeTagCategory != null) {
        val spec = TagUtils.getCategoryUiSpec(activeTagCategory)
        Triple(spec.containerColor, contentColorFor(spec.containerColor), spec.color)
    } else {
        Triple(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Color.Transparent)
    }

    val badgeIconRes = when (activeTagCategory) {
        TagCategory.Spaces -> R.drawable.view_cozy_24px_fill
        TagCategory.People -> R.drawable.person_24px_fill
        TagCategory.Topics -> R.drawable.cards_stack_24px_fill
        null -> null
    }

    val contentDescription = when {
        currentTab == HomeTab.Tags && activeTag != null -> {
            val cleanTag = TagUtils.getCleanTagName(activeTag)
            when (activeTagCategory) {
                TagCategory.Spaces -> "New entry in $cleanTag"
                TagCategory.People -> "New entry with @$cleanTag"
                TagCategory.Topics -> "New entry with #$cleanTag"
                null -> stringResource(R.string.new_journal)
            }
        }
        currentTab == HomeTab.Tags -> "New entry in ${selectedCategory.label}"
        else -> stringResource(R.string.new_journal)
    }

    return remember(activeTagCategory, activeTag, selectedCategory, containerColor, contentColor, badgeColor, badgeIconRes, contentDescription) {
        HomeFabConfig(badgeIconRes, badgeColor, containerColor, contentColor, contentDescription)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeBottomBar(
    pagerState: PagerState,
    selectedCategory: TagCategory,
    activeTag: String?,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val boundsTransform = remember {
        BoundsTransform { _, _ -> animationSpec }
    }

    val currentTab = HomeTab.entries[pagerState.currentPage]
    val fabConfig = rememberHomeFabConfig(currentTab, selectedCategory, activeTag)

    val animatedFabContainerColor by animateColorAsState(
        targetValue = fabConfig.containerColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "fab_container_color"
    )
    val animatedFabContentColor by animateColorAsState(
        targetValue = fabConfig.contentColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "fab_content_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer(
                        shadowElevation = with(LocalDensity.current) { 6.dp.toPx() },
                        shape = CircleShape,
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
            ) {
                LookaheadScope {
                    val pill = remember {
                        movableContentOf<BoxScope> { boxScope ->
                            with(boxScope) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .animateBounds(
                                            lookaheadScope = this@LookaheadScope,
                                            boundsTransform = boundsTransform,
                                        )
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        HomeTab.entries.forEachIndexed { index, tab ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier.zIndex(if (isSelected) 0f else 1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    pill(this)
                                }
                                Column(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .selectable(
                                            selected = isSelected,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            },
                                            role = Role.Tab,
                                            indication = null,
                                            interactionSource = null,
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    CompositionLocalProvider(
                                        LocalContentColor provides if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(if (isSelected) tab.filledIconRes else tab.iconRes),
                                            contentDescription = stringResource(tab.labelRes),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
            ) {
                FloatingActionButton(
                    onClick = onFabClick,
                    shape = MaterialTheme.shapes.large,
                    containerColor = animatedFabContainerColor,
                    contentColor = animatedFabContentColor,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_2_24px),
                        contentDescription = fabConfig.contentDescription,
                        modifier = Modifier.size(26.dp),
                    )
                }

                AnimatedContent(
                    targetState = fabConfig.badgeIconRes,
                    transitionSpec = {
                        fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                            scaleIn(tween(200, easing = FastOutSlowInEasing), initialScale = 0.25f) togetherWith
                            fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                            scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.25f)
                    },
                    label = "fab_badge_swap",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp),
                ) { badgeRes ->
                    if (badgeRes != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    painter = painterResource(badgeRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = fabConfig.badgeColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}