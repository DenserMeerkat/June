package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneCloseButton
import com.denser.june.presentation.components.JuneMoreOptionsButton
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.plus.LocalIsPlus
import com.denser.june.presentation.utils.UiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopAppBar(
    modifier: Modifier = Modifier,
    isFontMode: Boolean,
    isColorMode: Boolean = false,
    showAddItemSheet: Boolean,
    showSaveButton: Boolean,
    isLoading: Boolean,
    onCloseClick: () -> Unit,
    onFontModeToggle: () -> Unit,
    onColorModeToggle: () -> Unit = {},
    onAddItemClick: () -> Unit,
    onSaveClick: () -> Unit,
    onOptionsClick: () -> Unit,
    isFontDirty: Boolean = false,
    onResetFont: () -> Unit = {},
    isThemeDirty: Boolean = false,
    onResetTheme: () -> Unit = {},
    isPlusStylingActive: Boolean = false,
) {
    val isPlus = LocalIsPlus.current

    JuneTopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            if (isFontMode || isColorMode) {
                JuneCloseButton(onClick = onCloseClick)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JuneCloseButton(onClick = onCloseClick)

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

                    FilledIconButton(
                        onClick = onColorModeToggle,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.format_paint_24px),
                            stringResource(R.string.journal_theme)
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
                FilledIconButton(
                    onClick = onCloseClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        painterResource(R.drawable.check_24px),
                        stringResource(R.string.action_done)
                    )
                }
            } else if (isColorMode) {
                if (isThemeDirty) {
                    FilledIconButton(
                        onClick = onResetTheme,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.replay_24px),
                            stringResource(R.string.reset_theme)
                        )
                    }
                }
                FilledIconButton(
                    onClick = onCloseClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        painterResource(R.drawable.check_24px),
                        stringResource(R.string.action_done)
                    )
                }
            } else {
                if (showSaveButton) {
                    Button(
                        enabled = !isLoading,
                        onClick = onSaveClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        if (isPlusStylingActive && !isPlus) {
                            Icon(
                                painter = painterResource(UiUtils.PLUS_ICON_RES),
                                contentDescription = "Plus",
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.action_save),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Box {
                    JuneMoreOptionsButton(onClick = onOptionsClick)
                }
            }
        }
    )
}
