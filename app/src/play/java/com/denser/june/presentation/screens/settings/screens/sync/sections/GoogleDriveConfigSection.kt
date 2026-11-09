@file:Suppress("DEPRECATION")

package com.denser.june.presentation.screens.settings.screens.sync.sections

import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList
import com.denser.june.presentation.components.JuneConnectedRowItem
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem
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
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isConnected) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
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
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(stringResource(PlayR.string.sign_in_with_google))
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
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

                                    JuneDropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        tonalElevation = 6.dp,
                                    ) {
                                        if (folderUrl != null) {
                                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                            JuneDropdownMenuItem(
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
                                        JuneDropdownMenuItem(
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 4.dp,
                                bottomStart = 20.dp,
                                bottomEnd = 20.dp
                            )
                        )
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
    val privacyItems = remember {
        listOf(
            PrivacyCardData(
                iconRes = R.drawable.devices_24px,
                titleRes = PlayR.string.direct_connection,
                descRes = PlayR.string.direct_connection_desc
            ),
            PrivacyCardData(
                iconRes = R.drawable.lock_24px,
                titleRes = PlayR.string.sandboxed_storage,
                descRes = PlayR.string.sandboxed_storage_desc
            ),
            PrivacyCardData(
                iconRes = R.drawable.visibility_off_24px,
                titleRes = PlayR.string.no_server_tracking,
                descRes = PlayR.string.no_server_tracking_desc
            ),
            PrivacyCardData(
                iconRes = R.drawable.person_24px,
                titleRes = PlayR.string.profile_and_email,
                descRes = PlayR.string.profile_and_email_desc
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            item {
                Text(
                    text = stringResource(PlayR.string.privacy_and_data_security),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 6.dp)
                )
                Text(
                    text = stringResource(PlayR.string.privacy_and_data_security_desc),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    privacyItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 20.dp,
                                            topEnd = 20.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 20.dp,
                                            bottomEnd = 20.dp
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = stringResource(item.descRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class PrivacyCardData(
    val iconRes: Int,
    val titleRes: Int,
    val descRes: Int
)

