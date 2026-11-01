package com.denser.june.presentation.screens.settings.components.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.BuildConfig
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val packageInfo = remember {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
    }
    val sha256 = remember {
        try {
            val signatures = packageInfo.signingInfo?.apkContentsSigners
            if (!signatures.isNullOrEmpty()) {
                val certBytes = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val fingerprintBytes = md.digest(certBytes)
                fingerprintBytes.joinToString(":") { byte ->
                    String.format("%02X", byte)
                }
            } else {
                "No signatures found"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    val versionName = remember { packageInfo.versionName ?: "Unknown" }
    val versionCode = remember { packageInfo.longVersionCode }
    val deviceModel = remember { "${Build.MANUFACTURER} ${Build.MODEL}" }
    val androidOS = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
    val supportedAbis = remember { Build.SUPPORTED_ABIS.joinToString(", ") }

    val diagnosticsText = remember(sha256, versionName, versionCode, deviceModel, androidOS, supportedAbis) {
        """
        App Name: June
        Package Name: ${context.packageName}
        Version Name: $versionName
        Version Code: $versionCode
        Signing Key SHA-256: $sha256
        Hyphen Version: ${BuildConfig.HYPHEN_VERSION}
        
        Device: $deviceModel
        Android OS: $androidOS
        Supported ABIs: $supportedAbis
        """.trimIndent()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.diagnostics),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    item {
                        JuneConnectedList {
                            JuneConnectedItem(
                                title = stringResource(R.string.pkg_name),
                                subtitle = context.packageName,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            JuneConnectedItem(
                                title = stringResource(R.string.version_name),
                                subtitle = versionName,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            JuneConnectedItem(
                                title = stringResource(R.string.version_code),
                                subtitle = versionCode.toString(),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            JuneConnectedItem(
                                title = stringResource(R.string.signing_key_sha256),
                                subtitle = sha256,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            JuneConnectedItem(
                                title = stringResource(R.string.hyphen_version),
                                subtitle = BuildConfig.HYPHEN_VERSION,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        JuneConnectedList {
                            JuneConnectedItem(
                                title = stringResource(R.string.device),
                                subtitle = deviceModel,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            JuneConnectedItem(
                                title = stringResource(R.string.android_os),
                                subtitle = androidOS,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            JuneConnectedItem(
                                title = stringResource(R.string.supported_abis),
                                subtitle = supportedAbis,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        }
                    }
                }
            }

            JuneFloatingActionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                JuneFloatingAction(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("June App Diagnostics", diagnosticsText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    label = stringResource(R.string.action_copy),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.content_copy_24px),
                            contentDescription = stringResource(R.string.action_copy),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                JuneFloatingAction(
                    onClick = onDismissRequest,
                    label = stringResource(R.string.action_done),
                    selected = false,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.check_24px),
                            contentDescription = stringResource(R.string.action_done),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

