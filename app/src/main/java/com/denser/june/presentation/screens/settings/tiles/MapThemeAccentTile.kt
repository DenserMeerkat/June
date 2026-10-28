package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.presentation.components.pro.ProSettingsItem
import com.denser.june.presentation.pro.LocalIsPro
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MapThemeAccentTile(
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    val journalPreferences = koinInject<JournalPreferences>()
    val scope = rememberCoroutineScope()
    val isPro = LocalIsPro.current
    val prefEnabled by journalPreferences.isMapThemeAccentEnabled()
        .collectAsStateWithLifecycle(initialValue = true)

    val isChecked = checked ?: prefEnabled

    val handleToggle: (Boolean) -> Unit = { newValue ->
        if (onCheckedChange != null) {
            onCheckedChange(newValue)
        } else if (isPro) {
            scope.launch {
                journalPreferences.setMapThemeAccentEnabled(newValue)
            }
        }
    }

    ProSettingsItem(
        title = stringResource(R.string.map_theme_accents),
        subtitle = stringResource(R.string.map_theme_accents_desc),
        iconRes = R.drawable.palette_24px,
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = handleToggle
            )
        },
        onClick = {
            handleToggle(!isChecked)
        }
    )
}
