package com.denser.june.presentation.components.color

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

@Composable
fun PaletteCard(
    name: String,
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.check_circle_24px_fill),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            val outerRadius = 5.dp
            val innerRadius = 2.dp

            colors.forEachIndexed { index, color ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(
                        topStart = outerRadius,
                        bottomStart = outerRadius,
                        topEnd = innerRadius,
                        bottomEnd = innerRadius
                    )
                    colors.lastIndex -> RoundedCornerShape(
                        topStart = innerRadius,
                        bottomStart = innerRadius,
                        topEnd = outerRadius,
                        bottomEnd = outerRadius
                    )
                    else -> RoundedCornerShape(innerRadius)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color, shape)
                )
            }
        }
    }
}
