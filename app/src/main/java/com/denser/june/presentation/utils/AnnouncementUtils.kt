package com.denser.june.presentation.utils

import androidx.compose.ui.platform.UriHandler
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route

enum class AnnouncementActionType {
    WEB_URL,
    IN_APP_ROUTE
}

data class AnnouncementAction(
    val text: String,
    val type: AnnouncementActionType,
    val target: String
)

data class AnnouncementEntry(
    val id: String,
    val title: String,
    val message: String,
    val icon: String? = null,
    val minVersion: String? = null,
    val maxVersion: String? = null,
    val action: AnnouncementAction? = null
)

fun handleAnnouncementAction(
    action: AnnouncementAction,
    navigator: AppNavigator,
    uriHandler: UriHandler
) {
    when (action.type) {
        AnnouncementActionType.WEB_URL -> {
            uriHandler.openUri(action.target)
        }
        AnnouncementActionType.IN_APP_ROUTE -> {
            when (action.target.lowercase()) {
                "sync" -> navigator.navigateTo(Route.SyncSettings)
                "settings" -> navigator.navigateTo(Route.Settings)
                "about", "language", "translate" -> navigator.navigateTo(Route.AboutSettings)
                else -> navigator.navigateTo(Route.Settings)
            }
        }
    }
}
