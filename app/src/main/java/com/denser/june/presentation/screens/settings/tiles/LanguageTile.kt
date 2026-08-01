package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.core.utils.LanguageHelper
import com.denser.june.presentation.screens.settings.components.LanguageBottomSheet
import com.denser.june.presentation.screens.settings.components.SettingsItem

@Composable
fun LanguageTile() {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    val currentNativeName = remember(context) { LanguageHelper.getCurrentLanguageNativeName(context) }

    SettingsItem(
        title = stringResource(R.string.language),
        subtitle = currentNativeName,
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.language_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = {
            if (!LanguageHelper.openAppLanguageSettings(context)) {
                showBottomSheet = true
            }
        }
    )

    if (showBottomSheet) {
        LanguageBottomSheet(
            onDismiss = { showBottomSheet = false },
            onLanguageSelected = { code ->
                LanguageHelper.setAppLanguage(code)
            }
        )
    }
}
