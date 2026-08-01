package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.components.SettingsItem
import com.denser.june.presentation.screens.settings.components.TranslateBottomSheet

@Composable
fun TranslateTile() {
    var showSheet by remember { mutableStateOf(false) }

    SettingsItem(
        title = stringResource(R.string.translate_app),
        subtitle = stringResource(R.string.help_translate_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.language_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = {
            showSheet = true
        }
    )

    if (showSheet) {
        TranslateBottomSheet(
            onDismiss = { showSheet = false }
        )
    }
}
