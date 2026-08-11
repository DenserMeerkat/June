package com.denser.june.presentation.utils

import android.content.Context
import com.denser.june.BuildConfig
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.screens.settings.components.VersionEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray

import com.denser.june.presentation.utils.AnnouncementEntry
import com.denser.june.presentation.utils.AnnouncementAction
import com.denser.june.presentation.utils.AnnouncementActionType

class StartupManager(
    private val context: Context,
    private val privacyPrefs: PrivacyPreferences
) {
    private val _pendingWhatsChanged = MutableStateFlow<VersionEntry?>(null)
    val pendingWhatsChanged: Flow<VersionEntry?> = _pendingWhatsChanged

    private val _pendingAnnouncement = MutableStateFlow<AnnouncementEntry?>(null)
    val pendingAnnouncement: Flow<AnnouncementEntry?> = _pendingAnnouncement

    suspend fun checkStartupFlows() {
        val latestEntry = try {
            val jsonString = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            var foundEntry: VersionEntry? = null
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val version = obj.getString("version")
                val changesArray = obj.getJSONArray("changes")
                val changesList = mutableListOf<String>()
                for (j in 0 until changesArray.length()) {
                    val element = changesArray.get(j)
                    if (element is String) {
                        changesList.add(element)
                    } else if (element is org.json.JSONObject) {
                        val text = element.getString("text")
                        val flavors = if (element.has("flavors") && !element.isNull("flavors")) {
                            val arr = element.getJSONArray("flavors")
                            val fList = mutableListOf<String>()
                            for (k in 0 until arr.length()) {
                                fList.add(arr.getString(k))
                            }
                            fList
                        } else {
                            null
                        }
                        if (flavors == null || flavors.contains(BuildConfig.FLAVOR)) {
                            changesList.add(text)
                        }
                    }
                }
                if (changesList.isNotEmpty()) {
                    foundEntry = VersionEntry(version, changesList)
                    break
                }
            }
            foundEntry
        } catch (e: Exception) {
            null
        }

        if (latestEntry != null) {
            val lastShown = privacyPrefs.getLastChangelogShownFlow().first()
            if (lastShown != latestEntry.version) {
                _pendingWhatsChanged.value = latestEntry
            }
        }

        checkAnnouncement()
    }

    private suspend fun checkAnnouncement() {
        val latestAnnouncement = try {
            val jsonString = context.assets.open("announcements.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            if (jsonArray.length() > 0) {
                val obj = jsonArray.getJSONObject(0)
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

                AnnouncementEntry(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    message = obj.getString("message"),
                    icon = obj.optString("icon", null),
                    minVersion = obj.optString("minVersion", null),
                    maxVersion = obj.optString("maxVersion", null),
                    action = action
                )
            } else null
        } catch (e: Exception) {
            null
        }

        if (latestAnnouncement != null) {
            val lastDismissedId = privacyPrefs.getLastAnnouncementDismissedIdFlow().first()
            if (lastDismissedId != latestAnnouncement.id) {
                _pendingAnnouncement.value = latestAnnouncement
            }
        }
    }

    suspend fun dismissWhatsChanged(version: String) {
        privacyPrefs.updateLastChangelogShown(version)
        _pendingWhatsChanged.value = null
    }

    suspend fun dismissAnnouncement(id: String) {
        privacyPrefs.updateLastAnnouncementDismissedId(id)
        _pendingAnnouncement.value = null
    }
}
