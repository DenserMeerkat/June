package com.denser.june.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JunePlaceholderPage(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    icon: Int? = null,
    title: String = "",
    subtitle: String = "",
    iconSize: Dp = 64.dp,
    iconContainerSize: Dp = 100.dp,
    iconShape: Shape = RoundedCornerShape(32.dp),
    iconTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    fillMaxSize: Boolean = true,
    action: (@Composable () -> Unit)? = null
) {
    val layoutModifier = if (fillMaxSize) {
        modifier.fillMaxSize()
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = layoutModifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            ContainedLoadingIndicator(
                modifier = Modifier.size(iconContainerSize)
            )
        } else {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(iconContainerSize)
                        .clip(iconShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = iconTint
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            if (action != null) {
                Spacer(modifier = Modifier.height(16.dp))
                action()
            }
        }
    }
}