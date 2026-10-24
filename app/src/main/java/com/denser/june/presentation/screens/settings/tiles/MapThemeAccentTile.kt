package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.presentation.screens.settings.components.SettingsItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MapThemeAccentTile(
    onToggleChanged: ((Boolean) -> Unit)? = null
) {
    val journalPreferences = koinInject<JournalPreferences>()
    val scope = rememberCoroutineScope()
    val isEnabled by journalPreferences.isMapThemeAccentEnabled()
        .collectAsStateWithLifecycle(initialValue = true)

    SettingsItem(
        title = stringResource(R.string.map_theme_accents),
        subtitle = stringResource(R.string.map_theme_accents_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.palette_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Switch(
                checked = isEnabled,
                onCheckedChange = { newValue ->
                    scope.launch {
                        journalPreferences.setMapThemeAccentEnabled(newValue)
                        onToggleChanged?.invoke(newValue)
                    }
                }
            )
        },
        onClick = {
            val newValue = !isEnabled
            scope.launch {
                journalPreferences.setMapThemeAccentEnabled(newValue)
                onToggleChanged?.invoke(newValue)
            }
        }
    )
}
