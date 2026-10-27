package com.denser.june.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.preferences.PrivacyPreferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun InternetAccessDialog(
    onDismissRequest: () -> Unit
) {
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val isInternetAllowed by privacyPreferences.getIsInternetAllowedFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    var draftAllowed by remember { mutableStateOf<Boolean?>(null) }
    val currentAllowed = draftAllowed ?: isInternetAllowed ?: false
    val scope = rememberCoroutineScope()

    JuneDialog(
        onDismissRequest = onDismissRequest,
        icon = if (currentAllowed) R.drawable.wifi_24px else R.drawable.wifi_off_24px,
        title = stringResource(R.string.internet_access),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.internet_access_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Switch(
                        checked = currentAllowed,
                        onCheckedChange = { draftAllowed = it },
                        thumbContent = {
                            Icon(
                                painter = painterResource(
                                    if (currentAllowed) R.drawable.check_24px else R.drawable.close_24px
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.scale(1.4f)
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (draftAllowed != null && isInternetAllowed != null && draftAllowed != isInternetAllowed) {
                            privacyPreferences.updateIsInternetAllowed(draftAllowed!!)
                        }
                        onDismissRequest()
                    }
                }
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = null
    )
}

@Composable
fun InternetRestrictedIndicator(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.internet_access_disabled),
    description: String = stringResource(R.string.internet_restricted_desc),
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        InternetAccessDialog(
            onDismissRequest = { showDialog = false }
        )
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.wifi_off_24px),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showDialog = true }
        ) {
            Text(stringResource(R.string.permissions))
        }
    }
}

@Composable
fun InternetRestrictedBanner(
    modifier: Modifier = Modifier,
    description: String = stringResource(R.string.internet_access_restricted),
    useCompactSettingsButton: Boolean = false,
    onNavigateToSettings: (() -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        InternetAccessDialog(
            onDismissRequest = { showDialog = false }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.wifi_off_24px),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (useCompactSettingsButton) {
                IconButton(
                    onClick = { showDialog = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings_b_roll_24px),
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                TextButton(
                    onClick = { showDialog = true }
                ) {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
