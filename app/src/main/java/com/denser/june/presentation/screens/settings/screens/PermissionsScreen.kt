package com.denser.june.presentation.screens.settings.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsItem
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen() {
    val navigator = koinInject<AppNavigator>()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val privacyPreferences = koinInject<PrivacyPreferences>()
    val isInternetAllowed by privacyPreferences.getIsInternetAllowedFlow()
        .collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        hasLocationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                title = { Text(stringResource(R.string.permissions)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.permissions_desc_long),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            SettingSection {
                SettingsItem(
                    title = stringResource(R.string.internet_access),
                    subtitle = stringResource(R.string.internet_access_desc),
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.wifi_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isInternetAllowed,
                            onCheckedChange = { allowed ->
                                scope.launch { privacyPreferences.updateIsInternetAllowed(allowed) }
                            }
                        )
                    },
                    onClick = {
                        scope.launch { privacyPreferences.updateIsInternetAllowed(!isInternetAllowed) }
                    }
                )
                SettingsItem(
                    title = stringResource(R.string.location),
                    subtitle = if (hasLocationPermission)
                        stringResource(R.string.location_permission_granted)
                    else
                        stringResource(R.string.location_permission_needed),
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.location_on_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = {
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.permission_already_granted),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                )
                SettingsItem(
                    title = stringResource(R.string.notification),
                    subtitle = if (hasNotificationPermission)
                        stringResource(R.string.notification_permission_granted)
                    else
                        stringResource(R.string.notification_permission_needed),
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.notifications_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (!hasNotificationPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, context.getString(R.string.permission_already_granted), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.permission_already_granted), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.manage_in_settings))
                }
            }
            Spacer(modifier = Modifier.height(32.dp + padding.calculateBottomPadding()))
        }
    }
}