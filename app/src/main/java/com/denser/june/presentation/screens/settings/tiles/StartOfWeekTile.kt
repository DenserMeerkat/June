package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle

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
        JuneConnectedButtonGroup(
            options = DayOfWeek.entries,
            selected = { state.startOfWeek == it },
            onSelect = { viewModel.onAction(SettingsAction.OnStartOfWeekChange(it)) },
            label = { it.getDisplayName(TextStyle.NARROW, LocalLocale.current.platformLocale) },
            equalWeight = true,
            contentPadding = PaddingValues(4.dp),
            textStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
        )
    }
}
