package com.denser.june.presentation.screens.settings.tiles

import android.Manifest
import android.content.pm.PackageManager
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
fun LocationPermissionTile() {
    val context = LocalContext.current
    val permissionAlreadyGrantedMessage = stringResource(R.string.permission_already_granted)

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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
    }

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
                    permissionAlreadyGrantedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}
