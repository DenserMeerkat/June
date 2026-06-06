package com.denser.june.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.compose.ui.text.TextStyle

@Composable
fun JuneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: Int? = null,
    placeholder: String = "",
    placeholderStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = errorText != null,
            placeholder = { 
                Text(
                    text = placeholder,
                    style = placeholderStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                ) 
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (errorText != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingIcon = trailingIcon,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
        )
        AnimatedVisibility(
            visible = errorText != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            errorText?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }
    }
}
