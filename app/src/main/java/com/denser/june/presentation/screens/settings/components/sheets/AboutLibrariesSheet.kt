package com.denser.june.presentation.screens.settings.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedListShape

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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        val libs = libsState.value
        val libraryList = remember(libs) { libs?.libraries ?: emptyList() }

        if (libs == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.about_libraries),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.open_source_licenses_desc),
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                itemsIndexed(
                    items = libraryList,
                    key = { _, lib -> lib.uniqueId }
                ) { index, library ->
                    val firstLicense = remember(library) { library.licenses.firstOrNull() }
                    val licenseName = firstLicense?.name ?: stringResource(R.string.unknown_license)
                    val url = remember(library) { library.website ?: library.scm?.url }
                    val versionText = remember(library) { library.artifactVersion?.let { "v$it" } ?: "" }

                    val itemShape = remember(index, libraryList.size) {
                        when {
                            libraryList.size == 1 -> RoundedCornerShape(28.dp)
                            index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                            index == libraryList.lastIndex -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                    }

                    JuneConnectedItem(
                        title = library.name,
                        subtitle = versionText.ifEmpty { null },
                        shape = itemShape,
                        index = index,
                        count = libraryList.size,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                }
            }
        }
    }

    selectedLibraryForLicense?.let { library ->
        val firstLicense = library.licenses.firstOrNull()
        val licenseName = firstLicense?.name ?: stringResource(R.string.unknown_license)
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
                            text = licenseContent ?: stringResource(R.string.no_license_text_available),
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
