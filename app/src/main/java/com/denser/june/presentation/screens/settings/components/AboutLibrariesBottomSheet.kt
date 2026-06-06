package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.denser.june.core.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLibrariesBottomSheet(
    setShowSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val libsState = produceLibraries()
    val uriHandler = LocalUriHandler.current
    var selectedLibraryForLicense by remember { mutableStateOf<Library?>(null) }

    ModalBottomSheet(
        onDismissRequest = { setShowSheet(false) },
        modifier = modifier
    ) {
        val libs = libsState.value
        val libraryList = libs?.libraries ?: emptyList()

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.about_libraries),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Open Source Licenses & Dependencies",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            items(libraryList, key = { it.uniqueId }) { library ->
                val firstLicense = library.licenses.firstOrNull()
                val licenseName = firstLicense?.name ?: "Unknown License"
                val url = library.website ?: library.scm?.url
                val versionText = library.artifactVersion?.let { "v$it" } ?: "Unknown version"

                SettingsItem(
                    title = library.name,
                    subtitle = versionText,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    onClick = { selectedLibraryForLicense = library },
                    trailingContent = if (!url.isNullOrBlank()) {
                        {
                            IconButton(
                                onClick = { uriHandler.openUri(url) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.open_in_new_24px),
                                    contentDescription = "Open Website",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else null
                ) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = licenseName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    selectedLibraryForLicense?.let { library ->
        val firstLicense = library.licenses.firstOrNull()
        val licenseName = firstLicense?.name ?: "Unknown License"
        val licenseContent = firstLicense?.licenseContent

        LibraryLicenseBottomSheet(
            libraryName = library.name,
            licenseName = licenseName,
            licenseContent = licenseContent,
            onDismiss = { selectedLibraryForLicense = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryLicenseBottomSheet(
    libraryName: String,
    licenseName: String,
    licenseContent: String?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = libraryName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = licenseName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = licenseContent ?: "No license text available.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
