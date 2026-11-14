package com.denser.june.presentation.screens.settings.tiles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneSwitchTile

@Composable
fun LocationPermissionTile() {
    val context = LocalContext.current

    fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    var hasLocationPermission by remember { mutableStateOf(checkPermission()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasLocationPermission = checkPermission()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
    }

    val handleToggle = {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    JuneSwitchTile(
        title = stringResource(R.string.location),
        checked = hasLocationPermission,
        onCheckedChange = { handleToggle() },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.location_on_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    )
}
