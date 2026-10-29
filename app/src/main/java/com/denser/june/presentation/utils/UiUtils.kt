package com.denser.june.presentation.utils

import androidx.annotation.DrawableRes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

object UiUtils {

    val BOTTOM_BAR_PADDING = 100.dp

    @DrawableRes
    val PRO_ICON_RES = R.drawable.flower_24px_fill

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val proShape: Shape
        @Composable
        get() = MaterialShapes.Clover4Leaf.toShape()

    val proContainerColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.tertiaryContainer

    val proContentColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.onTertiaryContainer

    @Composable
    fun getTransparentTextFieldColors() = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface
    )
}