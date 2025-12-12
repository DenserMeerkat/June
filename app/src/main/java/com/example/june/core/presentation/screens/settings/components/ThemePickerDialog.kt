package com.example.june.core.presentation.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.june.R
import com.example.june.core.domain.enums.AppTheme
import com.example.june.core.presentation.screens.settings.SettingsAction
import com.example.june.core.presentation.screens.settings.SettingsState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemePickerDialog(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
          modifier = Modifier.widthIn(max = 280.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.routine_24px_fill),
                    contentDescription = "Select App Theme",
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = "Select App Theme",
                    style = MaterialTheme.typography.titleLarge
                )

                Column (
                    modifier = Modifier.widthIn(max = 250.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppTheme.entries.forEach { appTheme ->
                        ToggleButton(
                            modifier = Modifier.fillMaxWidth(),
                            checked = state.theme.appTheme == appTheme,
                            onCheckedChange = {
                                onAction(SettingsAction.OnThemeSwitch(appTheme))
                                onDismiss()
                            }
                        ) {
                            Text(text = stringResource(appTheme.stringRes))
                        }
                    }
                }
            }
        }
    }
}

