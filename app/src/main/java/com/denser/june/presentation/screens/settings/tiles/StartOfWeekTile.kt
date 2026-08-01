package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StartOfWeekTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.start_of_week),
        subtitle = state.startOfWeek.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.event_note_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.End)
        ) {
            Spacer(Modifier.width(16.dp))
            val days = DayOfWeek.entries
            days.forEachIndexed { index, day ->
                val isSelected = state.startOfWeek == day
                val shape = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    days.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { viewModel.onAction(SettingsAction.OnStartOfWeekChange(day)) },
                    shapes = shape,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                ) {
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, LocalLocale.current.platformLocale),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
