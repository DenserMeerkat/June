package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.koinInject

@Composable
fun BackupTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = stringResource(R.string.action_backup),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.upload_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
    )
}

@Composable
fun RestoreTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = stringResource(R.string.action_restore),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.download_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
    )
}

@Composable
fun AutoBackupTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = stringResource(R.string.auto_backup),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.settings_backup_restore),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
    )
}

@Composable
fun SettingsBackupTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = stringResource(R.string.settings_backup),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.rule_settings),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
    )
}

@Composable
fun ExportMarkdownTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = stringResource(R.string.export_markdown_archive),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.file_save_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
    )
}

@Composable
fun ImportMarkdownTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = stringResource(R.string.import_markdown),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.folder_open_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
    )
}

@Composable
fun ImportOtherAppsTile() {
    val navigator = koinInject<AppNavigator>()
    SettingsItem(
        title = stringResource(R.string.import_other_apps),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.book_5_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        },
        onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
    )
}
