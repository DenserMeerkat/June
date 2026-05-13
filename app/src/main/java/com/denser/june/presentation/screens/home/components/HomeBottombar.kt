package com.denser.june.presentation.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.presentation.screens.home.HomeTab
import kotlinx.coroutines.launch

import com.denser.june.core.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeBottomBar(
    pagerState: PagerState,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // FAB above the bottom navigation
            FloatingActionButton(
                onClick = onFabClick,
                shape = RoundedCornerShape(24.dp), // Squircle matching screenshot
                containerColor = MaterialTheme.colorScheme.primary, // Using primary for the cyan/blue look in screenshot
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(72.dp) // Large FAB
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit_24px), // Changing to edit icon as in screenshot
                    contentDescription = "New Journal",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Custom Bottom Navigation Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow, // Added subtle background color for navigation bar
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeTab.entries.forEachIndexed { index, tab ->
                        val isSelected = pagerState.currentPage == index

                        BottomNavItem(
                            selected = isSelected,
                            iconRes = tab.iconRes,
                            filledIconRes = tab.filledIconRes,
                            label = tab.label,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    filledIconRes: Int,
    label: String
) {
    val iconBackgroundColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val iconContentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            color = iconBackgroundColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .height(32.dp)
                .width(64.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(if (selected) filledIconRes else iconRes),
                    contentDescription = label,
                    tint = iconContentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor
        )
    }
}
