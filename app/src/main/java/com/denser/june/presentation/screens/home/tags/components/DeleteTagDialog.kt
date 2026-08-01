package com.denser.june.presentation.screens.home.tags.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.utils.TagUtils
import com.denser.june.presentation.components.JuneDialog

@Composable
fun DeleteTagDialog(
    tagName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    JuneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.delete_tag_title),
        icon = R.drawable.delete_24px,
        iconTint = MaterialTheme.colorScheme.error,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.about_to_delete),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InputChip(
                    selected = true,
                    onClick = { },
                    label = {
                        Text(
                            tagName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = TagUtils.getTagInputChipColors(tagName),
                    border = null
                )
                Text(
                    text = stringResource(R.string.delete_tag_warning)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}