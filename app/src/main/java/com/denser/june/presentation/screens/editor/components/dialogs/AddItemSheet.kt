package com.denser.june.presentation.screens.editor.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onDismiss: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onAddSongClick: () -> Unit,
    onAddLocationClick: () -> Unit
) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                spacing = 4.dp
            ) {
                AddItem(
                    icon = R.drawable.add_a_photo_24px,
                    text = stringResource(R.string.take_photo_or_video),
                    index = 0,
                    count = 4,
                    onClick = { onTakePhotoClick(); onDismiss() }
                )
                AddItem(
                    icon = R.drawable.add_photo_alternate_24px,
                    text = stringResource(R.string.add_photo_or_video),
                    index = 1,
                    count = 4,
                    onClick = { onAddPhotoClick(); onDismiss() }
                )
                AddItem(
                    icon = R.drawable.music_note_add_24px,
                    text = stringResource(R.string.add_song),
                    index = 2,
                    count = 4,
                    onClick = { onAddSongClick(); onDismiss() }
                )
                AddItem(
                    icon = R.drawable.add_location_24px,
                    text = stringResource(R.string.add_location_and_map),
                    index = 3,
                    count = 4,
                    onClick = { onAddLocationClick(); onDismiss() }
                )
            }
        }
    }
}

@Composable
fun AddItem(
    icon: Int,
    text: String,
    index: Int = 0,
    count: Int = 1,
    onClick: () -> Unit
) {
    JuneConnectedItem(
        title = text,
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
        },
        index = index,
        count = count,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        onClick = onClick
    )
}
