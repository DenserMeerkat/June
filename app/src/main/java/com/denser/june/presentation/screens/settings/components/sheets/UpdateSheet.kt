package com.denser.june.presentation.screens.settings.components.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route

data class UpdateSheetState(
    val showChecking: Boolean = false,
    val updateInfo: Triple<String, String, String>? = null,
    val showNoUpdate: Boolean = false,
    val errorMsg: String? = null,
    val showInternetDisabled: Boolean = false
) {
    val isAnyVisible: Boolean
        get() = showChecking || updateInfo != null || showNoUpdate || errorMsg != null || showInternetDisabled
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateBottomSheet(
    state: UpdateSheetState,
    onDismissChecking: () -> Unit,
    onDismissUpdateInfo: () -> Unit,
    onDismissNoUpdate: () -> Unit,
    onDismissError: () -> Unit,
    onDismissInternetDisabled: () -> Unit,
    navigator: AppNavigator
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val onDismissCurrent = {
        when {
            state.showChecking -> onDismissChecking()
            state.updateInfo != null -> onDismissUpdateInfo()
            state.showNoUpdate -> onDismissNoUpdate()
            state.showInternetDisabled -> onDismissInternetDisabled()
            state.errorMsg != null -> onDismissError()
        }
    }

    if (state.isAnyVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissCurrent,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    state.showChecking -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularWavyProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.checking_for_updates),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connecting to release server…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    state.updateInfo != null -> {
                        val (versionName, changelog, downloadUrl) = state.updateInfo
                        val isPlayStoreUpdate = downloadUrl.startsWith("market:") || downloadUrl.contains("play.google.com")

                        Text(
                            text = stringResource(R.string.update_available) + if (isPlayStoreUpdate) "" else " ($versionName)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = changelog,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        JuneFloatingActionBar(
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            JuneFloatingAction(
                                label = stringResource(R.string.later),
                                onClick = onDismissUpdateInfo,
                                selected = false
                            )
                            JuneFloatingAction(
                                label = if (isPlayStoreUpdate) "Update" else "Download",
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.download_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    onDismissUpdateInfo()
                                    if (downloadUrl.isNotBlank()) {
                                        try {
                                            uriHandler.openUri(downloadUrl)
                                        } catch (e: Exception) {
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    downloadUrl.toUri()
                                                )
                                            )
                                        }
                                    }
                                },
                                selected = null
                            )
                        }
                    }

                    state.showNoUpdate -> {
                        Icon(
                            painter = painterResource(R.drawable.check_circle_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.up_to_date),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.already_latest_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        JuneFloatingActionBar(
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            JuneFloatingAction(
                                label = stringResource(R.string.ok),
                                onClick = onDismissNoUpdate,
                                selected = null
                            )
                        }
                    }

                    state.showInternetDisabled -> {
                        Icon(
                            painter = painterResource(R.drawable.wifi_off_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.internet_access_disabled),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.internet_disabled_update_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        JuneFloatingActionBar(
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            JuneFloatingAction(
                                label = stringResource(R.string.cancel),
                                onClick = onDismissInternetDisabled,
                                selected = false
                            )
                            JuneFloatingAction(
                                label = stringResource(R.string.enable),
                                onClick = {
                                    onDismissInternetDisabled()
                                    navigator.navigateTo(Route.PrivacySecuritySettings)
                                },
                                selected = null
                            )
                        }
                    }

                    state.errorMsg != null -> {
                        Icon(
                            painter = painterResource(R.drawable.warning_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.update_check_failed),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        JuneFloatingActionBar(
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            JuneFloatingAction(
                                label = stringResource(R.string.okay),
                                onClick = onDismissError,
                                selected = null
                            )
                        }
                    }
                }
            }
        }
    }
}

