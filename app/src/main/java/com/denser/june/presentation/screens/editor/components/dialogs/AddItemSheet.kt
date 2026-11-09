package com.denser.june.presentation.screens.editor.components.dialogs

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

private data class AddItemOption(
    val icon: Int,
    val textRes: Int,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onAddAudioClick: () -> Unit,
    onAddSongClick: () -> Unit,
    onAddLocationClick: () -> Unit
) {
    val options = remember(onTakePhotoClick, onAddPhotoClick, onAddAudioClick, onAddSongClick, onAddLocationClick) {
        listOf(
            AddItemOption(R.drawable.add_a_photo_24px, R.string.take_photo_or_video, onTakePhotoClick),
            AddItemOption(R.drawable.add_photo_alternate_24px, R.string.add_photo_or_video, onAddPhotoClick),
            AddItemOption(R.drawable.mic_24px, R.string.add_or_record_audio, onAddAudioClick),
            AddItemOption(R.drawable.music_note_add_24px, R.string.add_song, onAddSongClick),
            AddItemOption(R.drawable.add_location_24px, R.string.add_location_and_map, onAddLocationClick)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape
                    )
            )

            Text(
                text = stringResource(R.string.add_item),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            JuneConnectedList(
                items = options,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                spacing = 4.dp
            ) { index, count, option ->
                JuneConnectedItem(
                    title = stringResource(option.textRes),
                    leadingContent = {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    index = index,
                    count = count,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                    onClick = {
                        option.onClick()
                        onDismiss()
                    }
                )
            }
        }
    }
}
