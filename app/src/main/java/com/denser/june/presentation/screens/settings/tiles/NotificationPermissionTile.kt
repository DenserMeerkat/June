package com.denser.june.presentation.screens.settings.tiles

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.components.SettingsItem

@Composable
fun NotificationPermissionTile() {
    val context = LocalContext.current
    val permissionAlreadyGrantedMessage = stringResource(R.string.permission_already_granted)

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

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
                    Toast.makeText(
                        context,
                        permissionAlreadyGrantedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    permissionAlreadyGrantedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}
