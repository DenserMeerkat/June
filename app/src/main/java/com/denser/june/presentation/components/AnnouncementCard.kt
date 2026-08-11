package com.denser.june.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.utils.AnnouncementEntry
import com.denser.june.presentation.utils.AnnouncementActionType

@Composable
fun AnnouncementCard(
    announcement: AnnouncementEntry,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                val iconRes = remember(announcement.icon) {
                    if (!announcement.icon.isNullOrBlank()) {
                        val resId = context.resources.getIdentifier(announcement.icon, "drawable", context.packageName)
                        if (resId != 0) resId else R.drawable.info_24px
                    } else {
                        R.drawable.info_24px
                    }
                }
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = announcement.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            announcement.action?.let { action ->
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = action.text)
                    Spacer(modifier = Modifier.width(6.dp))
                    val iconRes = when (action.type) {
                        AnnouncementActionType.WEB_URL -> R.drawable.open_in_new_24px
                        AnnouncementActionType.IN_APP_ROUTE -> R.drawable.arrow_forward_24px
                    }
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
