package com.denser.june.core.data.preferences

import android.content.Context
import com.denser.june.core.domain.preferences.EmojiPreferences
import org.json.JSONArray

class EmojiPreferencesImpl(
    private val context: Context,
) : EmojiPreferences {

    private companion object {
        const val PREFS_NAME = "emoji_prefs"
        const val KEY_RECENTS = "recent_emojis"
        const val MAX_RECENTS = 30
    }

    override fun getRecentEmojis(): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optString(i)
                    if (!item.isNullOrEmpty()) add(item)
                }
            }
        }.getOrDefault(emptyList())
    }

    override fun addRecentEmoji(emoji: String) {
        val current = getRecentEmojis().filter { it != emoji }.toMutableList()
        current.add(0, emoji)
        val trimmed = if (current.size > MAX_RECENTS) current.take(MAX_RECENTS) else current
        val jsonArray = JSONArray()
        trimmed.forEach { jsonArray.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENTS, jsonArray.toString())
            .apply()
    }

    override fun removeRecentEmoji(emoji: String) {
        val current = getRecentEmojis().filter { it != emoji }
        val jsonArray = JSONArray()
        current.forEach { jsonArray.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENTS, jsonArray.toString())
            .apply()
    }
}
