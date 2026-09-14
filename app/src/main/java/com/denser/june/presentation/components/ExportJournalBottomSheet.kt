package com.denser.june.presentation.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.markdown.MarkdownEngine
import com.denser.june.core.domain.model.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

enum class SingleExportOption {
    PACKAGE_ZIP,
    TEXT_ONLY_MD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportJournalBottomSheet(
    journal: Journal?,
    onDismiss: () -> Unit
) {
    if (journal == null) return

    val context = LocalContext.current
    val exportRepo = koinInject<ExportRepo>()
    val scope = rememberCoroutineScope()
    var selectedOption by remember { mutableStateOf(SingleExportOption.PACKAGE_ZIP) }
    var isPickerActive by remember { mutableStateOf(false) }

    val zipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tempZip = exportRepo.exportSingleJournalZip(journal).getOrNull()
                    if (tempZip != null && tempZip.exists()) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            tempZip.inputStream().use { input -> input.copyTo(output) }
                        }
                        tempZip.delete()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.journal_exported_successfully), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.failed_to_save_file), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.failed_to_save_file), Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        onDismiss()
                    }
                }
            }
        } else {
            onDismiss()
        }
    }

    val mdLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val mdContent = MarkdownEngine.toMarkdown(journal, relativeMediaPathPrefix = "media", includeMedia = false)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(mdContent.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.journal_exported_successfully), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.failed_to_save_file), Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        onDismiss()
                    }
                }
            }
        } else {
            onDismiss()
        }
    }

    if (journal.images.isEmpty()) {
        LaunchedEffect(journal.id) {
            val fileName = MarkdownEngine.generateFileName(journal, forSingleExport = true)
            mdLauncher.launch(fileName)
        }
    } else if (!isPickerActive) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.export_journal_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = stringResource(R.string.export_journal_has_media_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    JuneRadioTile(
                        selected = selectedOption == SingleExportOption.PACKAGE_ZIP,
                        title = stringResource(R.string.export_option_package_zip),
                        subtitle = stringResource(R.string.export_option_package_zip_desc),
                        badge = stringResource(R.string.recommended),
                        radioPosition = JuneRadioTilePosition.Trailing,
                        onClick = { selectedOption = SingleExportOption.PACKAGE_ZIP }
                    )

                    JuneRadioTile(
                        selected = selectedOption == SingleExportOption.TEXT_ONLY_MD,
                        title = stringResource(R.string.export_option_clean_text_md),
                        subtitle = stringResource(R.string.export_option_clean_text_md_desc),
                        radioPosition = JuneRadioTilePosition.Trailing,
                        onClick = { selectedOption = SingleExportOption.TEXT_ONLY_MD }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                JuneFloatingActionBar(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    JuneFloatingAction(
                        onClick = onDismiss,
                        label = stringResource(R.string.cancel),
                        icon = { Icon(painterResource(R.drawable.close_24px), contentDescription = null) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    JuneFloatingAction(
                        onClick = {
                            val baseFileName = MarkdownEngine.generateFileName(journal, forSingleExport = true)
                            val baseName = baseFileName.removeSuffix(".md")
                            isPickerActive = true
                            if (selectedOption == SingleExportOption.PACKAGE_ZIP) {
                                zipLauncher.launch("${baseName}.zip")
                            } else {
                                mdLauncher.launch("${baseName}.md")
                            }
                        },
                        label = stringResource(R.string.export_action),
                        icon = { Icon(painterResource(R.drawable.check_24px), contentDescription = null) }
                    )
                }
            }
        }
    }
}
