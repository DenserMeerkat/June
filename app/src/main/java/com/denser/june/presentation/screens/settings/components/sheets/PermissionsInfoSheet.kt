package com.denser.june.presentation.screens.settings.components.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList

private data class PermissionInfoItem(
    val titleRes: Int,
    val descRes: Int,
    val iconRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsInfoSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        PermissionInfoItem(
            titleRes = R.string.internet_access,
            descRes = R.string.internet_access_desc,
            iconRes = R.drawable.wifi_24px
        ),
        PermissionInfoItem(
            titleRes = R.string.location,
            descRes = R.string.location_permission_reason,
            iconRes = R.drawable.location_on_24px
        ),
        PermissionInfoItem(
            titleRes = R.string.notification,
            descRes = R.string.notification_permission_reason,
            iconRes = R.drawable.notifications_24px
        ),
        PermissionInfoItem(
            titleRes = R.string.microphone,
            descRes = R.string.microphone_permission_reason,
            iconRes = R.drawable.mic_24px
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.permissions_info_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 4.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.permissions_info_desc),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            item {
                JuneConnectedList {
                    items.forEach { item ->
                        JuneConnectedItem(
                            title = stringResource(item.titleRes),
                            subtitle = stringResource(item.descRes),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
