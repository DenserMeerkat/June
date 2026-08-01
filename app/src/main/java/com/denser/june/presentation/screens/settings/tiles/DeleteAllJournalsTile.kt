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
fun DeleteAllJournalsTile() {
    val triggers = LocalSettingsTriggers.current
    SettingsItem(
        title = stringResource(R.string.delete_all_journals),
        subtitle = stringResource(R.string.delete_all_journals_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.warning_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        onClick = { triggers.onDeleteAllJournals() }
    )
}
