package com.denser.june.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.denser.june.core.R
import com.denser.june.core.domain.model.PlatformLinks

fun PlatformLinks.toAvailableLinks(): List<Pair<String, String>> = listOfNotNull(
    spotify?.let { "Spotify" to it },
    appleMusic?.let { "Apple Music" to it },
    youtubeMusic?.let { "YouTube Music" to it },
    youtube?.let { "YouTube" to it },
    deezer?.let { "Deezer" to it },
    soundcloud?.let { "SoundCloud" to it },
    tidal?.let { "Tidal" to it },
    amazonMusic?.let { "Amazon Music" to it }
)

@Composable
fun ListenDropdownMenu(
    availableLinks: List<Pair<String, String?>>,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    trigger: @Composable () -> Unit
) {
    val context = LocalContext.current

    Box {
        trigger()

        JuneDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            offset = DpOffset(x = 0.dp, y = 4.dp),
        ) {
            if (availableLinks.isEmpty()) {
                JuneDropdownMenuItem(
                    text = { Text(stringResource(R.string.no_links_available)) },
                    onClick = onDismissRequest
                )
            } else {
                availableLinks.forEach { (platform, url) ->
                    JuneDropdownMenuItem(
                        text = {
                            Text(
                                text = platform,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onDismissRequest()
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, url?.toUri())
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(getPlatformIcon(platform)),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onDismissRequest()
                                    try {
                                        val clipboard =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Song Link", url)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT)
                                            .show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy_24px),
                                    contentDescription = "Copy Link",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ListenChip(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note_24px),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Listen",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun getPlatformIcon(platform: String): Int {
    return when (platform) {
        "Spotify" -> R.drawable.spotify
        "Apple Music" -> R.drawable.applemusic
        "YouTube Music" -> R.drawable.youtubemusic
        "YouTube" -> R.drawable.youtube
        "SoundCloud" -> R.drawable.soundcloud
        "Deezer" -> R.drawable.deezer
        "Tidal" -> R.drawable.tidal
        "Amazon Music" -> R.drawable.amazonmusic
        else -> R.drawable.music_note_24px
    }
}
