package com.denser.june.presentation.screens.settings.screens.sync.sections

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.R as PlayR
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.data.sync.GoogleDriveProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.style.TextAlign

@Composable
fun GoogleDriveConfigSection(
    isVisible: Boolean,
    isConnected: Boolean,
    status: SyncStatus,
    isTestingConnection: Boolean,
    onTestConnection: () -> Unit,
    onManualSync: () -> Unit,
    onRefresh: () -> Unit,
    onStopSync: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isSyncing = status is SyncStatus.Syncing || status is SyncStatus.Preparing
    val isAnyBusy = isSyncing || isTestingConnection
    var showPrivacySheet by remember { mutableStateOf(false) }

    val provider = koinInject<CloudProvider>(named("GoogleDrive")) as GoogleDriveProvider

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("JuneAuth", "Google Sign-In activity result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                android.util.Log.d("JuneAuth", "Google Sign-In account successfully retrieved: ${account?.email}")
                if (account != null) {
                    provider.handleSignIn(account)
                    onRefresh()
                }
            } catch (e: ApiException) {
                android.util.Log.e("JuneAuth", "Google Sign-In ApiException: status code = ${e.statusCode}, message = ${e.message}", e)
            }
        } else {
            android.util.Log.w("JuneAuth", "Google Sign-In not OK: result code = ${result.resultCode}")
        }
    }

    val accountEmail = remember(isConnected) {
        GoogleSignIn.getLastSignedInAccount(context)?.email ?: context.getString(PlayR.string.google_account)
    }
    val accountName = remember(isConnected) {
        GoogleSignIn.getLastSignedInAccount(context)?.displayName ?: context.getString(PlayR.string.connected)
    }
    val accountAvatar = remember(isConnected) {
        GoogleSignIn.getLastSignedInAccount(context)?.photoUrl?.toString()
    }
    val folderUrl by provider.folderUrl.collectAsState(initial = null)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!isConnected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_to_drive_24px),
                        contentDescription = "Link Google Drive",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(PlayR.string.link_google_drive),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(PlayR.string.link_google_drive_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            android.util.Log.d("JuneAuth", "Sign In button clicked. Launching Google Sign-In intent.")
                            launcher.launch(googleSignInClient.signInIntent)
                        },
                        enabled = !isAnyBusy,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(stringResource(PlayR.string.sign_in_with_google))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .clickable { showPrivacySheet = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your journals are private & secure.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            painter = painterResource(R.drawable.info_24px),
                            contentDescription = "Privacy Details",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (accountAvatar != null) {
                                AsyncImage(
                                    model = accountAvatar,
                                    contentDescription = "Google Avatar",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.drive_export_24px),
                                    contentDescription = "Linked",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                             Column(modifier = Modifier.weight(1f)) {
                                 Text(
                                     text = accountName,
                                     style = MaterialTheme.typography.bodyLarge,
                                     fontWeight = FontWeight.Bold,
                                     color = MaterialTheme.colorScheme.onSurface
                                 )
                                 Text(
                                     text = accountEmail,
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                             }

                             var showMenu by remember { mutableStateOf(false) }

                             Box {
                                 OutlinedIconButton(
                                     onClick = { showMenu = true },
                                     modifier = Modifier.size(36.dp),
                                     border = BorderStroke(
                                         1.dp,
                                         MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                     )
                                 ) {
                                     Icon(
                                         painter = painterResource(R.drawable.settings_24px),
                                         contentDescription = "Settings",
                                         modifier = Modifier.size(18.dp)
                                     )
                                 }

                                 DropdownMenu(
                                     expanded = showMenu,
                                     onDismissRequest = { showMenu = false },
                                     shape = RoundedCornerShape(24.dp),
                                     containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                     tonalElevation = 6.dp,
                                     modifier = Modifier.padding(horizontal = 8.dp)
                                 ) {
                                     if (folderUrl != null) {
                                         val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                         DropdownMenuItem(
                                             modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                             text = { Text(stringResource(R.string.open_folder), style = MaterialTheme.typography.labelLarge) },
                                             onClick = {
                                                 showMenu = false
                                                 uriHandler.openUri(folderUrl!!)
                                             },
                                             leadingIcon = {
                                                 Icon(
                                                     painter = painterResource(R.drawable.folder_open_24px),
                                                     contentDescription = null,
                                                     modifier = Modifier.size(20.dp)
                                                 )
                                             }
                                         )
                                     }
                                     DropdownMenuItem(
                                         modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                         text = { Text(stringResource(R.string.disconnect), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error) },
                                         onClick = {
                                             showMenu = false
                                             coroutineScope.launch {
                                                 provider.disconnect()
                                                 onRefresh()
                                             }
                                         },
                                         leadingIcon = {
                                             Icon(
                                                 painter = painterResource(R.drawable.logout_24px),
                                                 contentDescription = null,
                                                 modifier = Modifier.size(20.dp),
                                                 tint = MaterialTheme.colorScheme.error
                                             )
                                         }
                                     )
                                 }
                             }
                        }

                        OutlinedButton(
                            onClick = onTestConnection,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAnyBusy,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.verifying_connection))
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.backup_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(R.string.test_connection))
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isSyncing) {
                            onStopSync?.invoke()
                        } else {
                            onManualSync()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = if (isSyncing) true else !isTestingConnection,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    colors = if (isSyncing) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    if (isSyncing) {
                        Icon(
                            painterResource(R.drawable.close_24px),
                            contentDescription = stringResource(R.string.stop_sync),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.stop_sync), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.sync_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.sync_now), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (showPrivacySheet) {
        GoogleDrivePrivacyBottomSheet(
            onDismissRequest = { showPrivacySheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDrivePrivacyBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Privacy & Data Security",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                )
                Text(
                    text = "Google shows standard alerts when signing in. Here is what actually happens with your data:",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            item {
                PrivacyItem(
                    iconRes = R.drawable.person_24px,
                    title = stringResource(PlayR.string.profile_and_email),
                    description = stringResource(PlayR.string.profile_and_email_desc)
                )
            }

            item {
                PrivacyItem(
                    iconRes = R.drawable.sync_24px,
                    title = stringResource(PlayR.string.direct_connection),
                    description = stringResource(PlayR.string.direct_connection_desc)
                )
            }

            item {
                PrivacyItem(
                    iconRes = R.drawable.folder_open_24px,
                    title = stringResource(PlayR.string.sandboxed_storage),
                    description = stringResource(PlayR.string.sandboxed_storage_desc)
                )
            }

            item {
                PrivacyItem(
                    iconRes = R.drawable.security_24px,
                    title = stringResource(PlayR.string.no_server_tracking),
                    description = stringResource(PlayR.string.no_server_tracking_desc)
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PrivacyItem(
    iconRes: Int,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

