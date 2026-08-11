package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import androidx.core.net.toUri

data class UpdateDialogState(
    val showChecking: Boolean = false,
    val updateInfo: Triple<String, String, String>? = null,
    val showNoUpdate: Boolean = false,
    val errorMsg: String? = null,
    val showInternetDisabled: Boolean = false
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateDialogs(
    state: UpdateDialogState,
    onDismissChecking: () -> Unit,
    onDismissUpdateInfo: () -> Unit,
    onDismissNoUpdate: () -> Unit,
    onDismissError: () -> Unit,
    onDismissInternetDisabled: () -> Unit,
    navigator: AppNavigator
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    if (state.showChecking) {
        JuneDialog(
            onDismissRequest = onDismissChecking,
            title = stringResource(R.string.checking_for_updates),
            confirmButton = {},
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularWavyProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    state.updateInfo?.let { (versionName, changelog, downloadUrl) ->
        val isPlayStoreUpdate = downloadUrl.startsWith("market:") || downloadUrl.contains("play.google.com")
        JuneDialog(
            onDismissRequest = onDismissUpdateInfo,
            title = stringResource(R.string.update_available) + if (isPlayStoreUpdate) "" else " ($versionName)",
            text = {
                Column {
                    Text(
                        text = if (isPlayStoreUpdate) {
                            stringResource(R.string.update_available_play_store)
                        } else {
                            stringResource(R.string.update_available_foss)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isPlayStoreUpdate && changelog.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissUpdateInfo()
                        if (downloadUrl.isNotBlank()) {
                            try {
                                uriHandler.openUri(downloadUrl)
                            } catch (e: Exception) {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    downloadUrl.toUri()))
                            }
                        }
                    }
                ) {
                    Text(if (isPlayStoreUpdate) "Update" else "Download")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissUpdateInfo) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }

    if (state.showNoUpdate) {
        JuneDialog(
            onDismissRequest = onDismissNoUpdate,
            title = stringResource(R.string.up_to_date),
            text = { Text(stringResource(R.string.already_latest_version)) },
            confirmButton = {
                Button(onClick = onDismissNoUpdate) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (state.showInternetDisabled) {
        JuneDialog(
            onDismissRequest = onDismissInternetDisabled,
            title = stringResource(R.string.internet_access_disabled),
            text = { Text(stringResource(R.string.internet_disabled_update_notice)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissInternetDisabled()
                        navigator.navigateTo(Route.Permissions)
                    }
                ) {
                    Text(stringResource(R.string.enable))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissInternetDisabled) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    state.errorMsg?.let { errorMsg ->
        JuneDialog(
            onDismissRequest = onDismissError,
            title = stringResource(R.string.update_check_failed),
            text = { Text(errorMsg) },
            confirmButton = {
                Button(onClick = onDismissError) {
                    Text(stringResource(R.string.okay))
                }
            }
        )
    }
}
