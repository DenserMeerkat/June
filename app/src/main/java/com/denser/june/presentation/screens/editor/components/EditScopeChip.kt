package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import kotlinx.coroutines.launch

enum class EditSongScope { Library, JournalOnly }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScopeChip(
    scope: EditSongScope,
    modifier: Modifier = Modifier
) {
    val isLibrary = scope == EditSongScope.Library
    val label = if (isLibrary) "Library" else "Journal"
    val tooltipText = if (isLibrary) {
        stringResource(R.string.scope_affects_library)
    } else {
        stringResource(R.string.scope_affects_journal_only)
    }
    val icon = if (isLibrary) R.drawable.music_note_24px else R.drawable.book_5_24px

    val tooltipState = rememberTooltipState()
    val coroutineScope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below
        ),
        tooltip = {
            PlainTooltip(
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        },
        state = tooltipState
    ) {
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    coroutineScope.launch {
                        tooltipState.show()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = if (isLibrary) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
            },
            contentColor = if (isLibrary) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
