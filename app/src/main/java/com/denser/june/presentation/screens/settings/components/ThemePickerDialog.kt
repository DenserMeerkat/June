package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.denser.june.presentation.components.JuneFullScreenDialog
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemePickerDialog(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    JuneFullScreenDialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
          modifier = Modifier.widthIn(max = 280.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.routine_24px_fill),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Select App Theme",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column (
                    modifier = Modifier.widthIn(max = 250.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ThemeMode.entries.forEach { appTheme ->
                        ToggleButton(
                            modifier = Modifier.fillMaxWidth(),
                            checked = state.appTheme.themeMode == appTheme,
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

