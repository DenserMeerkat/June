package com.denser.june.presentation.screens.editor.components.emoji

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SquircleShape = RoundedCornerShape(10.dp)
private val EmojiTextStyle = TextStyle(fontSize = 30.sp, textAlign = TextAlign.Center)
private val VariantTextStyle = TextStyle(fontSize = 26.sp, textAlign = TextAlign.Center)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiTile(
    emoji: String,
    variants: List<BundledEmoji>,
    selected: Boolean,
    hasVariants: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromRecents: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var showRemoveMenu by remember { mutableStateOf(false) }
    val haptic = if (hasVariants || onRemoveFromRecents != null) LocalHapticFeedback.current else null
    val indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    val clickModifier = if (onRemoveFromRecents != null) {
        Modifier.combinedClickable(
            onClick = { onClick(emoji) },
            onLongClick = {
                haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                showRemoveMenu = true
            },
        )
    } else if (hasVariants) {
        Modifier.combinedClickable(
            onClick = { onClick(emoji) },
            onLongClick = {
                haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
        )
    } else {
        Modifier.clickable(onClick = { onClick(emoji) })
    }

    val backgroundModifier = if (selected) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer, SquircleShape)
    } else {
        Modifier
    }

    val indicatorPath = remember(hasVariants, onRemoveFromRecents) {
        if (hasVariants && onRemoveFromRecents == null) Path() else null
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .then(backgroundModifier)
            .then(clickModifier)
            .drawBehind {
                if (indicatorPath != null) {
                    val triangleSize = 5.5.dp.toPx()
                    val margin = 1.5.dp.toPx()
                    val right = size.width - margin
                    val bottom = size.height - margin
                    indicatorPath.reset()
                    indicatorPath.moveTo(right, bottom - triangleSize)
                    indicatorPath.lineTo(right, bottom)
                    indicatorPath.lineTo(right - triangleSize, bottom)
                    indicatorPath.close()
                    drawPath(indicatorPath, indicatorColor)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = emoji, style = EmojiTextStyle)

        if (showRemoveMenu && onRemoveFromRecents != null) {
            DropdownMenu(
                modifier = Modifier
                    .defaultMinSize(minWidth = 200.dp)
                    .padding(horizontal = 8.dp),
                expanded = true,
                onDismissRequest = { showRemoveMenu = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
            ) {
                DropdownMenuItem(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    text = { Text(stringResource(com.denser.june.core.R.string.remove)) },
                    onClick = {
                        showRemoveMenu = false
                        onRemoveFromRecents()
                    },
                    leadingIcon = {
                        Icon(painterResource(com.denser.june.core.R.drawable.delete_24px), null)
                    },
                )
            }
        }

        if (expanded && onRemoveFromRecents == null) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { expanded = false },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    variants.forEach { variant ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    expanded = false
                                    onClick(variant.emoji)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(text = variant.emoji, style = VariantTextStyle)
                        }
                    }
                }
            }
        }
    }
}
