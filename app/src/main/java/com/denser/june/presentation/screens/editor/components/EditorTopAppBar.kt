package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopAppBar(
    modifier: Modifier = Modifier,
    isFontMode: Boolean,
    showAddItemSheet: Boolean,
    hasCustomFont: Boolean,
    showSaveButton: Boolean,
    isLoading: Boolean,
    onCloseClick: () -> Unit,
    onFontModeToggle: () -> Unit,
    onAddItemClick: () -> Unit,
    onSaveClick: () -> Unit,
    onOptionsClick: () -> Unit,
    isFontDirty: Boolean = false,
    onResetFont: () -> Unit = {},
) {
    JuneTopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            if (isFontMode) {
                FilledIconButton(
                    onClick = onCloseClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                ) {
                    Icon(
                        painterResource(R.drawable.close_24px),
                        stringResource(R.string.close)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onCloseClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.close_24px),
                            stringResource(R.string.close)
                        )
                    }

                    FilledIconButton(
                        onClick = onAddItemClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(if (showAddItemSheet) R.drawable.add_circle_24px_fill else R.drawable.add_circle_24px),
                            stringResource(R.string.add_attachment)
                        )
                    }

                    FilledIconButton(
                        onClick = onFontModeToggle,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.brand_family_24px),
                            stringResource(R.string.journal_font)
                        )
                    }
                }
            }
        },
        actions = {
            if (isFontMode) {
                if (isFontDirty) {
                    FilledIconButton(
                        onClick = onResetFont,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.replay_24px),
                            stringResource(R.string.reset_font)
                        )
                    }
                }
            } else {
                if (showSaveButton) {
                    Button(
                        enabled = !isLoading,
                        onClick = onSaveClick
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }

                Box {
                    IconButton(
                        onClick = onOptionsClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.more_vert_24px),
                            stringResource(R.string.options)
                        )
                    }
                }
            }
        }
    )
}
