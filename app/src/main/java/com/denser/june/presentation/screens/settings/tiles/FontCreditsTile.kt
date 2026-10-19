package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.components.LocalSettingsTriggers
import com.denser.june.presentation.screens.settings.components.SettingsItem

@Composable
fun FontCreditsTile() {
    val triggers = LocalSettingsTriggers.current
    SettingsItem(
        title = stringResource(R.string.font_credits),
        subtitle = stringResource(R.string.font_credits_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.brand_family_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = { triggers.onFontCreditsClick() }
    )
}
