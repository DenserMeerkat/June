package com.denser.june.presentation.screens.home.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toShortMonth

@Composable
fun TimelineDateColumn(
    dateTime: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(48.dp)
            .padding(end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dateTime.toDayOfMonth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = dateTime.toShortMonth(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}
