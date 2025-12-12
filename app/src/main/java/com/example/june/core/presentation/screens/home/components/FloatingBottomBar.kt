package com.example.june.core.presentation.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.june.core.presentation.screens.home.NavItem

@Composable
fun FloatingBottomBar(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
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
                        elevation = 4.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 6.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem.entries.forEach { item ->
                        NavigationBarItem(
                            item = item,
                            isSelected = selectedItem == item,
                            onClick = { onItemSelected(item) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Surface(
                onClick = onFabClick,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                tonalElevation = 6.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Action",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationBarItem(
    item: NavItem,
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
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}