package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.denser.june.core.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayOfWeekPickerDialog(
    currentDay: DayOfWeek,
    onSelect: (DayOfWeek) -> Unit,
    onDismiss: () -> Unit
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
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.event_note_24px),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Start of the week",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val label = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        ToggleButton(
                            modifier = Modifier.fillMaxWidth(),
                            checked = day == currentDay,
                            onCheckedChange = {
                                onSelect(day)
                                onDismiss()
                            }
                        ) {
                            Text(text = label)
                        }
                    }
                }
            }
        }
    }
}
