package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressPhotosTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.compress_photos),
        subtitle = stringResource(R.string.compress_photos_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.photo_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Switch(
                checked = state.isCompressPhotosEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.OnCompressPhotosToggle(it)) }
            )
        },
        onClick = { viewModel.onAction(SettingsAction.OnCompressPhotosToggle(!state.isCompressPhotosEnabled)) },
        content = {
            AnimatedVisibility(
                visible = state.isCompressPhotosEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    JuneConnectedButtonGroup(
                        options = listOf("Low" to 68, "Medium" to 76, "High" to 84),
                        selected = { (_, quality) ->
                            when (quality) {
                                68 -> state.photoCompressionQuality <= 70
                                76 -> state.photoCompressionQuality in 71..80
                                else -> state.photoCompressionQuality > 80
                            }
                        },
                        onSelect = { (_, quality) ->
                            viewModel.onAction(SettingsAction.OnPhotoQualityChange(quality))
                        },
                        label = { (name, _) -> name },
                        equalWeight = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressVideosTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsItem(
        title = stringResource(R.string.compress_videos),
        subtitle = stringResource(R.string.compress_videos_desc),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.videocam_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Switch(
                checked = state.isCompressVideosEnabled,
                onCheckedChange = { viewModel.onAction(SettingsAction.OnCompressVideosToggle(it)) }
            )
        },
        onClick = { viewModel.onAction(SettingsAction.OnCompressVideosToggle(!state.isCompressVideosEnabled)) },
        content = {
            AnimatedVisibility(
                visible = state.isCompressVideosEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    JuneConnectedButtonGroup(
                        options = listOf("480p" to 480, "720p" to 720, "1080p" to 1080),
                        selected = { (_, target) -> state.videoCompressionQuality == target },
                        onSelect = { (_, target) ->
                            viewModel.onAction(SettingsAction.OnVideoQualityChange(target))
                        },
                        label = { (name, _) -> name },
                        equalWeight = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                    )
                }
            }
        }
    )
}
