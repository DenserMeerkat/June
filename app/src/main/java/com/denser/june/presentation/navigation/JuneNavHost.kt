package com.denser.june.presentation.navigation

import android.util.Log

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ContentTransform
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.denser.june.presentation.components.JuneMediaLightbox
import com.denser.june.presentation.screens.editor.EditorAction
import com.denser.june.presentation.screens.editor.EditorScreen
import com.denser.june.presentation.screens.editor.EditorVM
import com.denser.june.presentation.screens.editor.screens.AddSongScreen
import com.denser.june.presentation.screens.editor.screens.ItemGalleryScreen
import com.denser.june.presentation.screens.home.HomeScreen
import com.denser.june.presentation.screens.settings.SettingsScreen
import com.denser.june.presentation.screens.settings.screens.AboutSettingsScreen
import com.denser.june.presentation.screens.settings.screens.AppearanceSettingsScreen
import com.denser.june.presentation.screens.settings.screens.AppLockSettingsScreen
import com.denser.june.presentation.screens.settings.screens.ConsoleLogsScreen
import com.denser.june.presentation.screens.settings.screens.DataSyncSettingsScreen
import com.denser.june.presentation.screens.settings.screens.DeveloperSettingsScreen
import com.denser.june.presentation.screens.settings.screens.EditorSettingsScreen
import com.denser.june.presentation.screens.settings.screens.FontSelectionScreen
import com.denser.june.presentation.screens.settings.screens.GeneralSettingsScreen
import com.denser.june.presentation.screens.settings.screens.MapSettingsScreen
import com.denser.june.presentation.screens.settings.screens.PasswordSetupScreen
import com.denser.june.presentation.screens.settings.screens.PatternSetupScreen
import com.denser.june.presentation.screens.settings.screens.PinSetupScreen
import com.denser.june.presentation.screens.settings.screens.PrivacySecuritySettingsScreen
import com.denser.june.presentation.screens.settings.screens.RecoverySetupScreen
import com.denser.june.presentation.screens.settings.screens.VaultLockSettingsScreen
import com.denser.june.presentation.screens.settings.screens.reminder.ReminderScreen
import com.denser.june.presentation.screens.settings.screens.sync.SyncScreen
import com.denser.june.presentation.screens.settings.screens.trash.BinScreen
import com.denser.june.presentation.screens.settings.screens.vault.VaultScreen
import com.denser.june.presentation.theme.JuneThemeWrapper
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JuneNavHost(
    backStack: List<Route>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val navigator = koinInject<AppNavigator>()
    val koin = getKoin()

    val editorRoute = backStack.filterIsInstance<Route.Editor>().lastOrNull()
    var editorSession by remember { mutableStateOf<EditorSession?>(null) }

    if (editorRoute != null && editorSession?.route != editorRoute) {
        editorSession?.viewModelStore?.clear()
        val store = ViewModelStore()
        val storeOwner = object : ViewModelStoreOwner {
            override val viewModelStore = store
        }
        val vm = ViewModelProvider(
            storeOwner,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return koin.get<EditorVM> { parametersOf(editorRoute) } as T
                }
            }
        )[EditorVM::class.java]
        editorSession = EditorSession(editorRoute, store, vm)
    }

    LaunchedEffect(editorRoute) {
        if (editorRoute == null && editorSession != null) {
            editorSession?.viewModelStore?.clear()
            editorSession = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            editorSession?.viewModelStore?.clear()
            editorSession = null
        }
    }

    val currentEditorVM = editorSession?.viewModel ?: koinViewModel()

    val directionMultiplier = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = onBack,
        transitionSpec = {
            if (targetState.isLightbox() || initialState.isLightbox()) {
                fadeIn() togetherWith fadeOut()
            } else if (isForwardTransition(initialState, targetState, backStack)) {
                (slideInHorizontally(initialOffsetX = { directionMultiplier * it }))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { directionMultiplier * -it / 4 }) + fadeOut())
            } else {
                (slideInHorizontally(initialOffsetX = { directionMultiplier * -it / 4 }) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = { directionMultiplier * it }))
            }
        },
        popTransitionSpec = {
            if (targetState.isLightbox() || initialState.isLightbox()) {
                fadeIn() togetherWith fadeOut()
            } else if (isForwardTransition(initialState, targetState, backStack)) {
                (slideInHorizontally(initialOffsetX = { directionMultiplier * it }))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { directionMultiplier * -it / 4 }) + fadeOut())
            } else {
                (slideInHorizontally(initialOffsetX = { directionMultiplier * -it / 4 }) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = { directionMultiplier * it }))
            }
        },
        predictivePopTransitionSpec = { _ ->
            if (targetState.isLightbox() || initialState.isLightbox()) {
                fadeIn() togetherWith fadeOut()
            } else if (isForwardTransition(initialState, targetState, backStack)) {
                (slideInHorizontally(initialOffsetX = { directionMultiplier * it }))
                    .togetherWith(slideOutHorizontally(targetOffsetX = { directionMultiplier * -it / 4 }) + fadeOut())
            } else {
                (slideInHorizontally(initialOffsetX = { directionMultiplier * -it / 4 }) + fadeIn())
                    .togetherWith(slideOutHorizontally(targetOffsetX = { directionMultiplier * it }))
            }
        },
        entryProvider = entryProvider {
            entry<Route.Home> {
                HomeScreen()
            }

            entry<Route.Editor> {
                EditorScreen(viewModel = currentEditorVM)
            }

            entry<Route.JournalMedia> {
                val state by currentEditorVM.state.collectAsStateWithLifecycle()
                JuneThemeWrapper(
                    theme = state.theme,
                    font = state.font,
                    fontVariation = state.fontVariation
                ) {
                    ItemGalleryScreen(viewModel = currentEditorVM)
                }
            }

            entry<Route.AddSong> {
                val state by currentEditorVM.state.collectAsStateWithLifecycle()
                JuneThemeWrapper(
                    theme = state.theme,
                    font = state.font,
                    fontVariation = state.fontVariation
                ) {
                    AddSongScreen(
                        viewModel = currentEditorVM,
                        onNavigateBack = { navigator.navigateBack() }
                    )
                }
            }

            entry<Route.MediaViewerRoute>(
                metadata = lightboxMetadata
            ) { route ->
                JuneMediaLightbox(
                    mediaPaths = route.mediaPaths,
                    initialIndex = route.initialIndex,
                    dateTime = route.dateTime,
                    allowCompression = false
                )
            }

            entry<Route.JournalMediaDetail>(
                metadata = lightboxMetadata
            ) { route ->
                val state by currentEditorVM.state.collectAsStateWithLifecycle()
                val editorImages = remember(state.images) { state.images.reversed() }

                JuneThemeWrapper(
                    theme = state.theme,
                    font = state.font,
                    fontVariation = state.fontVariation
                ) {
                    JuneMediaLightbox(
                        mediaPaths = editorImages,
                        initialIndex = route.initialIndex,
                        dateTime = route.dateTime ?: state.dateTime,
                        journalId = route.journalId,
                        allowCompression = true,
                        onDeleteMedia = { path ->
                            currentEditorVM.onAction(EditorAction.RemoveImage(path))
                        }
                    )
                }
            }

            entry<Route.Settings> {
                SettingsScreen()
            }

            entry<Route.GeneralSettings> {
                GeneralSettingsScreen()
            }

            entry<Route.EditorSettings> {
                EditorSettingsScreen()
            }

            entry<Route.MapSettings> {
                MapSettingsScreen()
            }

            entry<Route.AppearanceSettings> {
                AppearanceSettingsScreen()
            }

            entry<Route.PrivacySecuritySettings> {
                PrivacySecuritySettingsScreen()
            }

            entry<Route.SyncBackupSettings> {
                DataSyncSettingsScreen()
            }

            entry<Route.AboutSettings> {
                AboutSettingsScreen()
            }

            entry<Route.AppLockSettings> {
                AppLockSettingsScreen()
            }

            entry<Route.VaultLockSettings> {
                VaultLockSettingsScreen()
            }

            entry<Route.PinSetup> { route ->
                PinSetupScreen(forVault = route.forVault)
            }

            entry<Route.PasswordSetup> { route ->
                PasswordSetupScreen(forVault = route.forVault)
            }

            entry<Route.PatternSetup> { route ->
                PatternSetupScreen(forVault = route.forVault)
            }

            entry<Route.RecoverySetup> { route ->
                RecoverySetupScreen(lockType = route.lockType, forVault = route.forVault)
            }

            entry<Route.Vault> {
                VaultScreen()
            }

            entry<Route.Bin> {
                BinScreen()
            }

            entry<Route.SyncSettings> {
                SyncScreen()
            }

            entry<Route.ReminderSettings> {
                ReminderScreen()
            }

            entry<Route.Fonts> { route ->
                FontSelectionScreen(type = route.type)
            }

            entry<Route.DeveloperSettings> {
                DeveloperSettingsScreen()
            }

            entry<Route.ConsoleLogs> {
                ConsoleLogsScreen()
            }
        }
    )
}

private class EditorSession(
    val route: Route.Editor,
    val viewModelStore: ViewModelStore,
    val viewModel: EditorVM
)

private val lightboxMetadata = NavDisplay.transitionSpec {
    fadeIn() togetherWith fadeOut()
} + NavDisplay.popTransitionSpec {
    fadeIn() togetherWith fadeOut()
} + NavDisplay.predictivePopTransitionSpec {
    fadeIn() togetherWith fadeOut()
}

private fun Scene<*>.isLightbox(): Boolean {
    val k = key
    if (k is Route.MediaViewerRoute || k is Route.JournalMediaDetail) return true
    if (k is Pair<*, *>) {
        val first = k.first.toString()
        val second = k.second.toString()
        if (first.contains("MediaViewerRoute") || first.contains("JournalMediaDetail") ||
            second.contains("MediaViewerRoute") || second.contains("JournalMediaDetail")
        ) {
            return true
        }
    }
    val ck = entries.lastOrNull()?.contentKey
    if (ck is Route.MediaViewerRoute || ck is Route.JournalMediaDetail) return true
    if (ck is Pair<*, *>) {
        val first = ck.first.toString()
        val second = ck.second.toString()
        if (first.contains("MediaViewerRoute") || first.contains("JournalMediaDetail") ||
            second.contains("MediaViewerRoute") || second.contains("JournalMediaDetail")
        ) {
            return true
        }
    }
    return false
}


private fun isForwardTransition(
    initialState: Scene<*>,
    targetState: Scene<*>,
    backStack: List<Route>
): Boolean {
    val initialIndex = backStack.indexOfLast { initialState.isForRoute(it) }
    val targetIndex = backStack.indexOfLast { targetState.isForRoute(it) }
    val isForward = when {
        initialIndex >= 0 && targetIndex >= 0 -> targetIndex > initialIndex
        initialIndex == -1 && targetIndex >= 0 -> false
        initialIndex >= 0 && targetIndex == -1 -> true
        else -> true
    }
    Log.d("JuneNavHost", "isForward: $isForward (initialIndex=$initialIndex, targetIndex=$targetIndex, initialKey=${initialState.key}, targetKey=${targetState.key})")
    return isForward
}

private fun Scene<*>.isForRoute(route: Route): Boolean {
    val targetString = route.toString()
    fun matches(k: Any?): Boolean {
        if (k == route) return true
        if (k is Pair<*, *>) {
            val first = k.first
            if (first == route || first == targetString) return true
        }
        return false
    }
    if (matches(key)) return true
    return entries.any { matches(it.contentKey) }
}
