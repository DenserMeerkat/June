package com.denser.june.presentation.screens.settings.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneBackButton
import com.denser.june.presentation.components.JuneConnectedItemShape
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsTileRegistry
import com.denser.june.presentation.screens.settings.components.sheets.PermissionsInfoSheet
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecuritySettingsScreen() {
    val context = LocalContext.current
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showPermissionsInfoSheet by remember { mutableStateOf(false) }

    val securityTiles = listOfNotNull(
        SettingsTileRegistry.getTile("APP_LOCK"),
        SettingsTileRegistry.getTile("VAULT_LOCK"),
        SettingsTileRegistry.getTile("SCREEN_PRIVACY")
    )
    val permissionTiles = listOfNotNull(
        SettingsTileRegistry.getTile("INTERNET_ACCESS"),
        SettingsTileRegistry.getTile("LOCATION_PERMISSION"),
        SettingsTileRegistry.getTile("NOTIFICATION_PERMISSION"),
        SettingsTileRegistry.getTile("MICROPHONE_PERMISSION")
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.privacy_and_security)) },
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        ) {
            item {
                SettingSection {
                    securityTiles.forEach { tile ->
                        tile.content()
                    }
                }
            }

            item {
                SettingSection(title = stringResource(R.string.permissions)) {
                    permissionTiles.forEach { tile ->
                        tile.content()
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = JuneConnectedItemShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(JuneConnectedItemShape)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.manage_in_settings),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(R.drawable.open_in_new_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .clip(CircleShape),
                                thickness = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )

                            Box(
                                modifier = Modifier
                                    .clickable { showPermissionsInfoSheet = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.info_24px),
                                    contentDescription = "Permission Details",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPermissionsInfoSheet) {
        PermissionsInfoSheet(
            onDismiss = { showPermissionsInfoSheet = false }
        )
    }
}


