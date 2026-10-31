package com.denser.june.presentation.screens.settings.components.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.utils.LanguageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanguageBottomSheet(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentCode = remember { LanguageHelper.getCurrentLanguageCode() }
    var selectedCode by remember { mutableStateOf(currentCode) }
    val supportedLanguages = remember(context) { LanguageHelper.getSupportedLanguages(context) }
    val systemDefault = supportedLanguages.firstOrNull { it.code.isEmpty() } ?: supportedLanguages.first()
    val otherLanguages = supportedLanguages.filter { it.code.isNotEmpty() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    val isSystemSelected = selectedCode.isEmpty()
                    SegmentedListItem(
                        onClick = {
                            selectedCode = ""
                            scope.launch {
                                delay(150.milliseconds)
                                onLanguageSelected("")
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                        selected = isSystemSelected,
                        shapes = ListItemDefaults.segmentedShapes(0, 1),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSystemSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        content = {
                            Text(
                                text = systemDefault.nativeName,
                                fontWeight = if (isSystemSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSystemSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = if (isSystemSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.check_24px),
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                itemsIndexed(otherLanguages, key = { _, item -> item.code }) { index, language ->
                    val isSelected = language.code.equals(selectedCode, ignoreCase = true)

                    SegmentedListItem(
                        onClick = {
                            selectedCode = language.code
                            scope.launch {
                                delay(150.milliseconds)
                                onLanguageSelected(language.code)
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                        selected = isSelected,
                        shapes = ListItemDefaults.segmentedShapes(index, otherLanguages.size),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        content = {
                            Text(
                                text = language.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.check_24px),
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
