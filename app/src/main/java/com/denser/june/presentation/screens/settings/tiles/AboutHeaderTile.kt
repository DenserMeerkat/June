package com.denser.june.presentation.screens.settings.tiles

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.BuildConfig
import com.denser.june.core.R
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.utils.Constants
import com.denser.june.presentation.components.ConnectedBarAction
import com.denser.june.presentation.components.JuneConnectedActionBar
import com.denser.june.presentation.screens.settings.components.LocalSettingsTriggers
import com.denser.june.presentation.screens.settings.components.sheets.TranslateBottomSheet
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
    var showTranslateSheet by remember { mutableStateOf(false) }
    val totalShapes = 8
    val currentShape = when (shapeIndex) {
        0 -> MaterialShapes.Cookie4Sided.toShape()
        1 -> MaterialShapes.Cookie6Sided.toShape()
        2 -> MaterialShapes.Cookie9Sided.toShape()
        3 -> MaterialShapes.Cookie7Sided.toShape()
        4 -> MaterialShapes.Clover4Leaf.toShape()
        5 -> MaterialShapes.Sunny.toShape()
        6 -> MaterialShapes.Clover8Leaf.toShape()
        7 -> MaterialShapes.Circle.toShape()
        else -> MaterialShapes.Cookie4Sided.toShape()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(currentShape)
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
                                    val toast = Toast.makeText(
                                        context,
                                        "Developer options enabled",
                                        Toast.LENGTH_SHORT
                                    )
                                    activeToast = toast
                                    toast.show()
                                } else if (tapCount >= 3) {
                                    val remaining = 7 - tapCount
                                    activeToast?.cancel()
                                    val toast = Toast.makeText(
                                        context,
                                        "You are now $remaining steps away from enabling developer options.",
                                        Toast.LENGTH_SHORT
                                    )
                                    activeToast = toast
                                    toast.show()
                                }
                            }
                        }
                    }
                )
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
                    .scale(1.25f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.brand_app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Connected button group with newsletter icon, version name, and check for update icon
        JuneConnectedActionBar(
            actions = listOf(
                ConnectedBarAction(
                    iconRes = R.drawable.newspaper_24px,
                    contentDescription = stringResource(R.string.changelog),
                    onClick = triggers.onChangelogClick
                ),
                ConnectedBarAction(
                    label = "v${BuildConfig.VERSION_NAME}",
                    onClick = triggers.onChangelogClick
                ),
                ConnectedBarAction(
                    iconRes = R.drawable.sync_24px,
                    contentDescription = stringResource(R.string.check_for_updates),
                    onClick = triggers.onCheckForUpdatesClick
                )
            ),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            buttonHeight = 36.dp,
            iconOnlyWidth = 42.dp,
            horizontalArrangement = Arrangement.spacedBy(
                ButtonGroupDefaults.ConnectedSpaceBetween,
                Alignment.CenterHorizontally
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Full-width connected button group with interaction effects: GitHub, Play Store, Translate, and License
        JuneConnectedActionBar(
            actions = listOf(
                ConnectedBarAction(
                    iconRes = R.drawable.github,
                    contentDescription = "GitHub",
                    onClick = { uriHandler.openUri(Constants.GITHUB_URL) }
                ),
                ConnectedBarAction(
                    iconRes = R.drawable.play_store,
                    contentDescription = "Play Store",
                    onClick = { uriHandler.openUri(Constants.PLAY_STORE_URL) }
                ),
                ConnectedBarAction(
                    iconRes = R.drawable.weblate,
                    contentDescription = "Translate",
                    onClick = { showTranslateSheet = true }
                ),
                ConnectedBarAction(
                    iconRes = R.drawable.gavel_24px,
                    contentDescription = stringResource(R.string.license),
                    onClick = triggers.onLicenseClick
                )
            ),
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            buttonHeight = 44.dp,
            iconSize = 22.dp,
            equalWeight = true,
            horizontalArrangement = Arrangement.spacedBy(
                ButtonGroupDefaults.ConnectedSpaceBetween,
                Alignment.CenterHorizontally
            )
        )
    }

    if (showTranslateSheet) {
        TranslateBottomSheet(
            onDismiss = { showTranslateSheet = false }
        )
    }
}
