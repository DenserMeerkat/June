package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.EditorLayoutDirection
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditorDirectionTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.editor_direction),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.format_textdirection_l_to_r_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    ) {
        JuneConnectedButtonGroup(
            options = EditorLayoutDirection.entries,
            selected = { state.editorLayoutDirection == it },
            onSelect = { viewModel.onAction(SettingsAction.OnEditorLayoutDirectionChange(it)) },
            label = {
                when (it) {
                    EditorLayoutDirection.AUTO -> "Auto"
                    EditorLayoutDirection.LTR -> "LTR"
                    EditorLayoutDirection.RTL -> "RTL"
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
