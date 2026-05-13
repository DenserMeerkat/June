package com.denser.june.presentation.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
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
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onFabClick,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_2_24px),
                        contentDescription = "New Journal"
                    )
                }
            },
        ) {
            HomeTab.entries.forEachIndexed { index, tab ->
                val isSelected = pagerState.currentPage == index

                ToolbarTab(
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

@Composable
private fun ToolbarTab(
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    filledIconRes: Int,
    label: String
) {
    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }

    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = CircleShape,
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .size(52.dp, 40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                painter = painterResource(if (selected)  filledIconRes else iconRes),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}