package com.denser.june.presentation.screens.settings.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppImportSheet(
    onDismiss: () -> Unit,
    onSelectFile: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val supportedApps = remember {
        listOf(
            SupportedAppInfo(R.string.brand_day_one, ".zip"),
            SupportedAppInfo(R.string.brand_diarium, ".diary, .diag, .db"),
            SupportedAppInfo(R.string.brand_daily_you, ".zip, .json"),
            SupportedAppInfo(R.string.brand_storypad, ".sp, .zip, .json")
        )
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.import_other_apps),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_import_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.supported_apps),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            JuneConnectedList(
                spacing = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                supportedApps.forEachIndexed { index, app ->
                    JuneConnectedItem(
                        title = stringResource(app.nameRes),
                        trailingContent = {
                            Text(
                                text = app.extensions,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        index = index,
                        count = supportedApps.size
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            JuneFloatingActionBar(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                JuneFloatingAction(
                    onClick = onDismiss,
                    label = stringResource(R.string.action_cancel),
                    icon = { Icon(painterResource(R.drawable.close_24px), contentDescription = null) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                JuneFloatingAction(
                    onClick = {
                        onDismiss()
                        onSelectFile()
                    },
                    label = stringResource(R.string.select_file),
                    icon = { Icon(painterResource(R.drawable.folder_open_24px), contentDescription = null) }
                )
            }
        }
    }
}

private data class SupportedAppInfo(
    val nameRes: Int,
    val extensions: String
)
