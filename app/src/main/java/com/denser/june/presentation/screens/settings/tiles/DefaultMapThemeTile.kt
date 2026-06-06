package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DefaultMapThemeTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = "Default map theme",
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.routine_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    ) {
        val themeModes = MapTheme.entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.End)
        ) {
            themeModes.forEachIndexed { index, mode ->
                val isSelected = state.mapTheme == mode
                val shape = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    themeModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }

                val iconRes = when (mode) {
                    MapTheme.APP -> R.drawable.devices_24px
                    MapTheme.LIGHT -> R.drawable.light_mode_24px
                    MapTheme.DARK -> R.drawable.dark_mode_24px
                }

                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { viewModel.onAction(SettingsAction.OnMapThemeChange(mode)) },
                    shapes = shape,
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    )
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = stringResource(mode.stringRes),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(mode.stringRes),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
