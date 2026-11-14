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
fun KeyboardAutocorrectTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    JuneSwitchTile(
        title = stringResource(R.string.keyboard_autocorrect),
        iconRes = R.drawable.spellcheck_24px,
        checked = state.isKeyboardAutocorrectEnabled,
        onCheckedChange = { viewModel.onAction(SettingsAction.OnKeyboardAutocorrectToggle(it)) }
    )
}
