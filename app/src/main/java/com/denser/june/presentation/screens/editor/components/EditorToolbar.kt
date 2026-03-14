package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.hyphen.model.MarkupStyle
import com.denser.hyphen.state.HyphenTextState
import com.denser.june.R

@Composable
fun EditorToolbar(
    state: HyphenTextState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatToggleButton(
                        icon = R.drawable.format_bold_24px,
                        contentDescription = "Bold",
                        isActive = state.hasStyle(MarkupStyle.Bold),
                        onClick = { state.toggleStyle(MarkupStyle.Bold) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_italic_24px,
                        contentDescription = "Italic",
                        isActive = state.hasStyle(MarkupStyle.Italic),
                        onClick = { state.toggleStyle(MarkupStyle.Italic) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_underlined_24px,
                        contentDescription = "Underline",
                        isActive = state.hasStyle(MarkupStyle.Underline),
                        onClick = { state.toggleStyle(MarkupStyle.Underline) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.strikethrough_s_24px,
                        contentDescription = "Strikethrough",
                        isActive = state.hasStyle(MarkupStyle.Strikethrough),
                        onClick = { state.toggleStyle(MarkupStyle.Strikethrough) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_ink_highlighter_24px,
                        contentDescription = "Highlight",
                        isActive = state.hasStyle(MarkupStyle.Highlight),
                        onClick = { state.toggleStyle(MarkupStyle.Highlight) }
                    )

                    VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    FormatToggleButton(
                        icon = R.drawable.format_quote_24px,
                        contentDescription = "Blockquote",
                        isActive = state.hasStyle(MarkupStyle.Blockquote),
                        onClick = { state.toggleStyle(MarkupStyle.Blockquote) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.code_24px,
                        contentDescription = "Inline Code",
                        isActive = state.hasStyle(MarkupStyle.InlineCode),
                        onClick = { state.toggleStyle(MarkupStyle.InlineCode) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_list_bulleted_24px,
                        contentDescription = "Bullet List",
                        isActive = state.hasStyle(MarkupStyle.BulletList),
                        onClick = { state.toggleStyle(MarkupStyle.BulletList) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_list_numbered_24px,
                        contentDescription = "Ordered List",
                        isActive = state.hasStyle(MarkupStyle.OrderedList),
                        onClick = { state.toggleStyle(MarkupStyle.OrderedList) }
                    )

                    VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    FormatToggleButton(
                        icon = R.drawable.format_h1_24px,
                        contentDescription = "Heading 1",
                        isActive = state.hasStyle(MarkupStyle.H1),
                        onClick = { state.toggleStyle(MarkupStyle.H1) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_h2_24px,
                        contentDescription = "Heading 2",
                        isActive = state.hasStyle(MarkupStyle.H2),
                        onClick = { state.toggleStyle(MarkupStyle.H2) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_h3_24px,
                        contentDescription = "Heading 3",
                        isActive = state.hasStyle(MarkupStyle.H3),
                        onClick = { state.toggleStyle(MarkupStyle.H3) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_h4_24px,
                        contentDescription = "Heading 4",
                        isActive = state.hasStyle(MarkupStyle.H4),
                        onClick = { state.toggleStyle(MarkupStyle.H4) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_h5_24px,
                        contentDescription = "Heading 5",
                        isActive = state.hasStyle(MarkupStyle.H5),
                        onClick = { state.toggleStyle(MarkupStyle.H5) }
                    )
                    FormatToggleButton(
                        icon = R.drawable.format_h6_24px,
                        contentDescription = "Heading 6",
                        isActive = state.hasStyle(MarkupStyle.H6),
                        onClick = { state.toggleStyle(MarkupStyle.H6) }
                    )

                    Spacer(Modifier.width(4.dp))
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            IconButton(
                onClick = { state.undo() },
                enabled = state.canUndo,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(painterResource(R.drawable.undo_24px), contentDescription = "Undo", modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = { state.redo() },
                enabled = state.canRedo,
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(painterResource(R.drawable.redo_24px), contentDescription = "Redo", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FormatToggleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    icon: Int,
    contentDescription: String? = null
) {
    IconToggleButton(
        checked = isActive,
        onCheckedChange = { onClick() },
        modifier = Modifier
            .size(40.dp)
            .focusProperties { canFocus = false },
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}