package com.denser.june.presentation.utils

import androidx.compose.ui.platform.UriHandler
import com.denser.june.BuildConfig
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import org.json.JSONArray

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
    val flavors: List<String>? = null,
    val action: AnnouncementAction? = null
)

fun parseAnnouncement(
    jsonString: String,
    currentFlavor: String = BuildConfig.FLAVOR
): AnnouncementEntry? {
    return try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val flavors = if (obj.has("flavors") && !obj.isNull("flavors")) {
                val arr = obj.getJSONArray("flavors")
                List(arr.length()) { arr.getString(it) }
            } else null

            if (flavors != null && !flavors.contains(currentFlavor)) {
                continue
            }

            val actionObj = obj.optJSONObject("action")
            val action = if (actionObj != null) {
                val typeStr = actionObj.getString("type")
                val type = try {
                    AnnouncementActionType.valueOf(typeStr.uppercase())
                } catch (e: Exception) {
                    AnnouncementActionType.WEB_URL
                }
                AnnouncementAction(
                    text = actionObj.getString("text"),
                    type = type,
                    target = actionObj.getString("target")
                )
            } else null

            return AnnouncementEntry(
                id = obj.getString("id"),
                title = obj.getString("title"),
                message = obj.getString("message"),
                icon = obj.optString("icon").takeIf { it.isNotEmpty() },
                minVersion = obj.optString("minVersion").takeIf { it.isNotEmpty() },
                maxVersion = obj.optString("maxVersion").takeIf { it.isNotEmpty() },
                flavors = flavors,
                action = action
            )
        }
        null
    } catch (e: Exception) {
        null
    }
}

fun handleAnnouncementAction(
    action: AnnouncementAction,
    navigator: AppNavigator,
    uriHandler: UriHandler,
    onOpenPlus: (() -> Unit)? = null
) {
    when (action.type) {
        AnnouncementActionType.WEB_URL -> {
            try {
                uriHandler.openUri(action.target)
            } catch (e: Exception) {
                // Ignore failure if URI scheme or browser activity is not resolved
            }
        }
        AnnouncementActionType.IN_APP_ROUTE -> {
            when (action.target.lowercase()) {
                "sync" -> navigator.navigateTo(Route.SyncSettings)
                "settings" -> navigator.navigateTo(Route.Settings)
                "about", "language", "translate" -> navigator.navigateTo(Route.AboutSettings)
                "plus", "june_plus", "paywall" -> onOpenPlus?.invoke() ?: navigator.navigateTo(Route.Settings)
                else -> navigator.navigateTo(Route.Settings)
            }
        }
    }
}
