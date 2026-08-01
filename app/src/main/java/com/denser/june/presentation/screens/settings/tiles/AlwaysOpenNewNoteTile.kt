package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlwaysOpenNewNoteTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.always_open_new_note),
        subtitle = stringResource(R.string.always_open_new_note_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.edit_note_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Switch(
                checked = state.alwaysOpenNewNote,
                onCheckedChange = { viewModel.onAction(SettingsAction.OnAlwaysOpenNewNoteToggle(it)) }
            )
        },
        onClick = { viewModel.onAction(SettingsAction.OnAlwaysOpenNewNoteToggle(!state.alwaysOpenNewNote)) }
    )
}
