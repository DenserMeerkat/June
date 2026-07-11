package com.denser.june.presentation.screens.settings.tiles

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.BuildConfig
import com.denser.june.core.R
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.utils.Constants
import com.denser.june.presentation.screens.settings.components.LocalSettingsTriggers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutHeaderTile() {
    val uriHandler = LocalUriHandler.current
    val triggers = LocalSettingsTriggers.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncPrefs = koinInject<SyncPreferences>()

    var shapeIndex by remember { mutableIntStateOf(0) }
    var tapCount by remember { mutableIntStateOf(0) }
    var activeToast by remember { mutableStateOf<Toast?>(null) }
    val totalShapes = 8
    val currentShape = when (shapeIndex) {
        0 -> MaterialShapes.Square.toShape()
        1 -> MaterialShapes.Cookie4Sided.toShape()
        2 -> MaterialShapes.Clover4Leaf.toShape()
        3 -> MaterialShapes.Sunny.toShape()
        4 -> MaterialShapes.Clover8Leaf.toShape()
        5 -> MaterialShapes.Circle.toShape()
        6 -> MaterialShapes.Cookie7Sided.toShape()
        7 -> MaterialShapes.VerySunny.toShape()
        else -> MaterialShapes.Square.toShape()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onLongClick = triggers.onAboutHeaderClick,
                onClick = {
                    shapeIndex = (shapeIndex + 1) % totalShapes
                    scope.launch {
                        val isDev = syncPrefs.isDeveloperModeEnabled().first()
                        if (!isDev) {
                            tapCount++
                            if (tapCount >= 7) {
                                syncPrefs.setDeveloperModeEnabled(true)
                                activeToast?.cancel()
                                val toast = Toast.makeText(context, "Developer options enabled", Toast.LENGTH_SHORT)
                                activeToast = toast
                                toast.show()
                            } else if (tapCount >= 3) {
                                val remaining = 7 - tapCount
                                activeToast?.cancel()
                                val toast = Toast.makeText(context, "You are now $remaining steps away from enabling developer options.", Toast.LENGTH_SHORT)
                                activeToast = toast
                                toast.show()
                            }
                        }
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(currentShape),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.25f),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { uriHandler.openUri(Constants.GITHUB_URL) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.github),
                    contentDescription = "Github Link",
                )
            }
        }
    }
}
