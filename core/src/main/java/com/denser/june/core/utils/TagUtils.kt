package com.denser.june.core.utils

import com.denser.june.core.domain.model.enums.TagCategory

object TagUtils {

    const val TAG_CHARACTER_LIMIT = 25

    fun getCategoryForTag(tag: String): TagCategory = TagCategory.fromTag(tag)

    fun getCleanTagName(tag: String): String = tag.removePrefix("@").removePrefix("#")

    fun appendPrefix(tag: String, category: TagCategory): String = when (category) {
        TagCategory.People -> "@$tag"
        TagCategory.Topics -> "#$tag"
        TagCategory.Spaces -> tag
    }

    fun filterTagsByCategory(tags: List<String>, category: TagCategory): List<String> =
        tags.filter { getCategoryForTag(it) == category }

    fun normalizeToTopic(rawTag: String?): String? {
        if (rawTag.isNullOrBlank()) return null
        val trimmed = rawTag.trim()
        val cleaned = trimmed.replace(Regex("[\\n\\r\\t]"), "")
        if (cleaned.isBlank()) return null

        val prefixed = when {
            cleaned.startsWith("@") -> "@" + cleaned.removePrefix("@").trim().trimStart('@')
            cleaned.startsWith("#") -> "#" + cleaned.removePrefix("#").trim().trimStart('#')
            else -> "#$cleaned"
        }

        return if (prefixed.length > TAG_CHARACTER_LIMIT) {
            prefixed.take(TAG_CHARACTER_LIMIT)
        } else {
            prefixed
        }
    }

    fun normalizeToSpace(rawCategory: String?): String? {
        if (rawCategory.isNullOrBlank()) return null
        val clean = rawCategory.trim()
            .removePrefix("@")
            .removePrefix("#")
            .trim()
        if (clean.isBlank()) return null

        return if (clean.length > TAG_CHARACTER_LIMIT) {
            clean.take(TAG_CHARACTER_LIMIT)
        } else {
            clean
        }
    }

    fun normalizeTags(
        tags: Collection<String>?,
        categorySpace: String? = null
    ): List<String> {
        val result = mutableListOf<String>()

        categorySpace?.let { space ->
            normalizeToSpace(space)?.let { result.add(it) }
        }

        tags?.forEach { raw ->
            normalizeToTopic(raw)?.let { result.add(it) }
        }

        return result.distinct()
    }
}
