package com.denser.june.core.domain.preferences

interface EmojiPreferences {
    fun getRecentEmojis(): List<String>
    fun addRecentEmoji(emoji: String)
    fun removeRecentEmoji(emoji: String)
}
