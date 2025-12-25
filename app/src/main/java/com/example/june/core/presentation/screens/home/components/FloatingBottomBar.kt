package com.example.june.core.presentation.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.june.R
import com.example.june.core.presentation.screens.home.HomeNavItem

@Composable
fun FloatingBottomBar(
    currentRoute: String,
    onItemSelected: (String) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .padding(top = 24.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Surface(
                modifier = Modifier
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f),
                        ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
                    ),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 6.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeNavItem.entries.forEach { item ->
                        NavigationBarItem(
                            item = item,
                            isSelected = currentRoute == item.route,
                            onClick = { onItemSelected(item.route) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            FloatingActionButton(
                onClick = onFabClick,
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_2_24px),
                    contentDescription = "New Action"
                )
            }
        }
    }
}

@Composable
fun NavigationBarItem(
    item: HomeNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(if (isSelected) 48.dp else 40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(if (isSelected) item.selectedIcon else item.icon),
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}