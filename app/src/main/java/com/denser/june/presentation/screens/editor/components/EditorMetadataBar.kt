package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import com.denser.june.presentation.components.ConnectedBarAction
import com.denser.june.presentation.components.JuneConnectedActionBar

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditorMetadataBar(
    emoji: String?,
    formattedDate: String?,
    formattedTime: String?,
    tagCount: Int,
    onEmojiClick: () -> Unit,
    onDatePickerOpen: () -> Unit,
    onTimePickerOpen: () -> Unit,
    onTagsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JuneConnectedActionBar(
            actions = listOf(
                ConnectedBarAction(
                    label = formattedDate,
                    onClick = onDatePickerOpen
                ),
                ConnectedBarAction(
                    iconRes = if (formattedTime == null) R.drawable.schedule_24px else null,
                    label = formattedTime,
                    contentDescriptionRes = R.string.time_format,
                    onClick = onTimePickerOpen
                )
            ),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            checkedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            buttonHeight = 32.dp,
            iconOnlyWidth = 38.dp
        )
        Spacer(modifier = Modifier.weight(1f))
        JuneConnectedActionBar(
            actions = listOf(
                ConnectedBarAction(
                    iconRes = if (emoji == null) R.drawable.mood_24px else null,
                    customContent = if (emoji != null) {
                        {
                            Text(
                                text = emoji,
                                fontSize = 16.sp
                            )
                        }
                    } else null,
                    contentDescriptionRes = R.string.add_emoji,
                    onClick = onEmojiClick,
                    isActive = emoji != null,
                    activeContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    activeContentColor = MaterialTheme.colorScheme.tertiary
                ),
                ConnectedBarAction(
                    iconRes = if (tagCount > 0) R.drawable.sell_24px_fill else R.drawable.sell_24px,
                    label = if (tagCount > 0) tagCount.toString() else null,
                    contentDescriptionRes = R.string.tags,
                    onClick = onTagsClick,
                    isActive = tagCount > 0
                )
            ),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContentColor = MaterialTheme.colorScheme.secondary,
            buttonHeight = 32.dp,
            iconOnlyWidth = 38.dp
        )
    }
}
