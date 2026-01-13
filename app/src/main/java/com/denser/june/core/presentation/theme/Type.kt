package com.denser.june.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.denser.june.R

@Composable
fun provideTypography(
    scale: Float = 1f,
    font: Int = R.font.poppins_regular,
): Typography {

    val displayFont = FontFamily(Font(font))
    val bodyFont = FontFamily(Font(font))

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp * scale,
            lineHeight = 64.sp * scale,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp * scale,
            lineHeight = 52.sp * scale,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp * scale,
            lineHeight = 44.sp * scale,
            letterSpacing = 0.sp
        ),

        headlineLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp * scale,
            lineHeight = 40.sp * scale,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp * scale,
            lineHeight = 36.sp * scale,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp * scale,
            lineHeight = 32.sp * scale,
            letterSpacing = 0.sp
        ),

        titleLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp * scale,
            lineHeight = 28.sp * scale,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp * scale,
            lineHeight = 24.sp * scale,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp * scale,
            lineHeight = 20.sp * scale,
            letterSpacing = 0.1.sp
        ),

        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp * scale,
            lineHeight = 24.sp * scale,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp * scale,
            lineHeight = 20.sp * scale,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp * scale,
            lineHeight = 16.sp * scale,
            letterSpacing = 0.4.sp
        ),

        labelLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp * scale,
            lineHeight = 20.sp * scale,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp * scale,
            lineHeight = 16.sp * scale,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp * scale,
            lineHeight = 16.sp * scale,
            letterSpacing = 0.5.sp
        )
    )
}