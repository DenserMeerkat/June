package com.denser.june.presentation.screens.settings.screens.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

@Composable
fun AppIconDisplay(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "App Icon",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.25f),
        )
    }
}

@Composable
fun LockHeader(
    title: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String = stringResource(R.string.wrong_pin_try_again),
    iconSpacing: Dp = 24.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconDisplay()
        Spacer(modifier = Modifier.height(iconSpacing))
        Text(
            text = if (isError) errorText else title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
