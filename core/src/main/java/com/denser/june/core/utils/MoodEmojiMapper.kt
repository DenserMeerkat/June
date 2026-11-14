package com.denser.june.core.utils

object MoodEmojiMapper {

    fun fromRating(rating: Int?): String? {
        return when (rating) {
            1 -> "😭"
            2 -> "🙁"
            3 -> "😐"
            4 -> "🙂"
            5 -> "😄"
            else -> null
        }
    }

    fun fromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val clean = text.trim().lowercase()

        if (clean.codePoints().anyMatch { Character.isSurrogate(it.toChar()) || Character.getType(it) == Character.OTHER_SYMBOL.toInt() }) {
            return text.trim()
        }

        return when {
            clean.contains("awful") || clean.contains("terrible") || clean.contains("depressed") || clean.contains("crying") -> "😭"
            clean.contains("angry") || clean.contains("mad") || clean.contains("furious") || clean.contains("frustrated") -> "😡"
            clean.contains("anxious") || clean.contains("nervous") || clean.contains("stressed") || clean.contains("worried") -> "😰"
            clean.contains("sad") || clean.contains("down") || clean.contains("lonely") || clean.contains("unhappy") -> "😔"
            clean.contains("tired") || clean.contains("exhausted") || clean.contains("sleepy") || clean.contains("drained") -> "😴"
            clean.contains("sick") || clean.contains("ill") || clean.contains("unwell") || clean.contains("fever") -> "🤒"
            clean.contains("meh") || clean.contains("okay") || clean.contains("fine") || clean.contains("neutral") -> "😐"
            clean.contains("calm") || clean.contains("peaceful") || clean.contains("relaxed") || clean.contains("chill") -> "😌"
            clean.contains("productive") || clean.contains("focused") || clean.contains("accomplished") -> "🎯"
            clean.contains("grateful") || clean.contains("blessed") || clean.contains("loved") || clean.contains("thankful") -> "🥰"
            clean.contains("rad") || clean.contains("awesome") || clean.contains("excited") || clean.contains("ecstatic") || clean.contains("great") -> "🤩"
            clean.contains("good") || clean.contains("happy") || clean.contains("content") || clean.contains("cheerful") -> "😊"
            else -> null
        }
    }

    fun fromWeather(weatherDescription: String?): String? {
        if (weatherDescription.isNullOrBlank()) return null
        val clean = weatherDescription.trim().lowercase()
        return when {
            clean.contains("clear") || clean.contains("sunny") -> "☀️"
            clean.contains("rain") || clean.contains("drizzle") || clean.contains("shower") -> "🌧️"
            clean.contains("thunder") || clean.contains("storm") || clean.contains("lightning") -> "⛈️"
            clean.contains("snow") || clean.contains("blizzard") || clean.contains("flurries") -> "❄️"
            clean.contains("fog") || clean.contains("mist") || clean.contains("haze") -> "🌫️"
            clean.contains("cloud") || clean.contains("overcast") -> "⛅"
            clean.contains("wind") || clean.contains("breezy") -> "💨"
            else -> null
        }
    }
}
