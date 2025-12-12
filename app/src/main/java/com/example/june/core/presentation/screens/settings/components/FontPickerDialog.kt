package com.example.june.core.presentation.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.june.R
import com.example.june.core.domain.enums.Fonts
import com.example.june.core.presentation.screens.settings.SettingsAction
import com.example.june.core.presentation.screens.settings.SettingsState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FontPickerDialog(
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
                    painter = painterResource(R.drawable.format_size_24px),
                    contentDescription = "Select App Font",
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = "Select App Font",
                    style = MaterialTheme.typography.titleLarge
                )

                Column (
                    modifier = Modifier.widthIn(max = 250.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Fonts.entries.forEach { font ->
                        ToggleButton(
                            modifier = Modifier.fillMaxWidth(),
                            checked = state.theme.font == font,
                            onCheckedChange = {
                                onAction(SettingsAction.OnFontChange(font))
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = font.fullName,
                                fontFamily = FontFamily(Font(font.font))
                            )
                        }
                    }
                }
            }
        }
    }
}

