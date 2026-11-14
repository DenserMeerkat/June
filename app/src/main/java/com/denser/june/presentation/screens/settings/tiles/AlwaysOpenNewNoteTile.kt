package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneSwitchTile
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlwaysOpenNewNoteTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    JuneSwitchTile(
        title = stringResource(R.string.always_open_new_note),
        iconRes = R.drawable.edit_note_24px,
        checked = state.alwaysOpenNewNote,
        onCheckedChange = { viewModel.onAction(SettingsAction.OnAlwaysOpenNewNoteToggle(it)) }
    )
}
