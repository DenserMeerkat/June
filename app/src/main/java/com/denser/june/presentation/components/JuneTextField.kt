package com.denser.june.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
private fun JuneInputContainer(
    modifier: Modifier = Modifier,
    label: String = "",
    errorText: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        content()
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

@Composable
fun defaultJuneTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedBorderColor = Color.Transparent,
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
)

@Composable
private fun createPlaceholder(
    text: String,
    style: TextStyle
): @Composable () -> Unit = {
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false
    )
}

@Composable
private fun createLeadingIcon(
    iconRes: Int?,
    isError: Boolean
): (@Composable () -> Unit)? = iconRes?.let { icon ->
    {
        Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun JuneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    placeholder: String = "",
    placeholderStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape = RoundedCornerShape(12.dp),
    interactionSource: MutableInteractionSource? = null,
    colors: TextFieldColors = defaultJuneTextFieldColors(),
    trailingIcon: @Composable (() -> Unit)? = null
) {
    JuneInputContainer(
        modifier = modifier,
        label = label,
        errorText = errorText
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = errorText != null,
            interactionSource = interactionSource,
            placeholder = createPlaceholder(placeholder, placeholderStyle),
            leadingIcon = createLeadingIcon(leadingIcon, errorText != null),
            trailingIcon = trailingIcon,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            shape = shape,
            colors = colors
        )
    }
}

@Composable
fun JuneSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String = "",
    leadingIcon: Int? = null,
    placeholder: String = "",
    placeholderStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.Hidden,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    onKeyboardAction: KeyboardActionHandler? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    interactionSource: MutableInteractionSource? = null,
    colors: TextFieldColors = defaultJuneTextFieldColors(),
    trailingIcon: @Composable (() -> Unit)? = null
) {
    JuneInputContainer(
        modifier = modifier,
        label = label,
        errorText = errorText
    ) {
        val hasError = isError || errorText != null
        OutlinedSecureTextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = hasError,
            textObfuscationMode = textObfuscationMode,
            interactionSource = interactionSource,
            placeholder = createPlaceholder(placeholder, placeholderStyle),
            leadingIcon = createLeadingIcon(leadingIcon, hasError),
            trailingIcon = trailingIcon,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            shape = shape,
            colors = colors
        )
    }
}
