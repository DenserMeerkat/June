// Adapted from https://github.com/bikram-agarwal/Remember/blob/main/app/src/main/java/dev/bikram/remember/ui/edit/IconPicker.kt

package com.denser.june.presentation.screens.editor.components.emoji

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.denser.june.core.R
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Immutable
@Serializable
data class BundledEmoji(
    val key: String,
    val emoji: String,
    val name: String,
    val slug: String,
    val category: String,
    val keywords: List<String> = emptyList(),
)

enum class EmojiCategory(
    val key: String,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val iconFilledRes: Int,
    @param:StringRes val labelRes: Int,
) {
    RECENT("recent", R.drawable.schedule_24px, R.drawable.schedule_24px, R.string.emoji_category_recent),
    SMILEYS_AND_EMOTIONS("smileys_and_emotions", R.drawable.mood_24px, R.drawable.mood_24px_fill, R.string.emoji_category_smileys),
    PEOPLE("people", R.drawable.emoji_people_24px, R.drawable.emoji_people_24px_fill, R.string.emoji_category_people),
    ANIMALS_AND_NATURE("animals_and_nature", R.drawable.emoji_nature_24px, R.drawable.emoji_nature_24px_fill, R.string.emoji_category_animals),
    FOOD_AND_DRINK("food_and_drink", R.drawable.emoji_food_beverage_24px, R.drawable.emoji_food_beverage_24px_fill, R.string.emoji_category_food),
    TRAVEL_AND_PLACES("travel_and_places", R.drawable.emoji_transportation_24px, R.drawable.emoji_transportation_24px_fill, R.string.emoji_category_travel),
    ACTIVITIES_AND_EVENTS("activities_and_events", R.drawable.trophy_24px, R.drawable.trophy_24px_fill, R.string.emoji_category_activities),
    OBJECTS("objects", R.drawable.emoji_objects_24px, R.drawable.emoji_objects_24px_fill, R.string.emoji_category_objects),
    SYMBOLS("symbols", R.drawable.emoji_symbols_24px, R.drawable.emoji_symbols_24px_fill, R.string.emoji_category_symbols),
    FLAGS("flags", R.drawable.flag_24px, R.drawable.flag_24px_fill, R.string.emoji_category_flags),
}

val emojiCategories = EmojiCategory.entries.toList()

@Immutable
data class EmojiDisplayEntry(
    val emoji: BundledEmoji,
    val variants: List<BundledEmoji>,
    val displayEmoji: String,
    val hasVariants: Boolean = variants.size > 1,
)

data class EmojiSkinToneGroupKey(
    val category: String,
    val baseName: String,
)

data class EmojiSkinToneGroup(
    val base: BundledEmoji,
    val variants: List<BundledEmoji>,
)

private val emojiSkinToneLabels = listOf(
    "light skin tone",
    "medium-light skin tone",
    "medium skin tone",
    "medium-dark skin tone",
    "dark skin tone",
)

data class EmojiSkinToneIndex(
    val groups: Map<EmojiSkinToneGroupKey, EmojiSkinToneGroup>,
    val keyByEmoji: Map<String, EmojiSkinToneGroupKey>,
)

data class EmojiAssets(
    val emojis: List<BundledEmoji>,
    val skinToneIndex: EmojiSkinToneIndex,
    val entriesByCategory: Map<String, List<EmojiDisplayEntry>>,
    val emojisByValue: Map<String, BundledEmoji>,
)

private val emojiJson = Json { ignoreUnknownKeys = true }
private val emojiAssetsLock = Any()

@Volatile
private var cachedEmojiAssets: EmojiAssets? = null

fun getCachedEmojiAssets(): EmojiAssets? = cachedEmojiAssets

suspend fun loadEmojiAssets(resources: Resources): EmojiAssets {
    cachedEmojiAssets?.let { return it }
    return withContext(Dispatchers.Default) {
        synchronized(emojiAssetsLock) {
            cachedEmojiAssets?.let { return@withContext it }
            val emojis = resources.openRawResource(com.denser.june.R.raw.emojis).bufferedReader().use { reader ->
                emojiJson.decodeFromString<List<BundledEmoji>>(reader.readText())
            }
            val skinToneIndex = buildEmojiSkinToneIndex(emojis)
            val collapsed = collapseEmojiSkinToneVariants(emojis, skinToneIndex)
            val byCategory = collapsed.groupBy { it.emoji.category }
            val byValue = emojis.associateBy { it.emoji }
            val assets = EmojiAssets(
                emojis = emojis,
                skinToneIndex = skinToneIndex,
                entriesByCategory = byCategory,
                emojisByValue = byValue,
            )
            cachedEmojiAssets = assets
            assets
        }
    }
}

fun buildEmojiSkinToneIndex(allEmojis: List<BundledEmoji>): EmojiSkinToneIndex {
    val groups = buildEmojiSkinToneGroups(allEmojis)
    val keyByEmoji = HashMap<String, EmojiSkinToneGroupKey>(allEmojis.size)
    for (emoji in allEmojis) {
        emojiSkinToneGroupKey(emoji, groups)?.let { groupKey ->
            keyByEmoji[emoji.emoji] = groupKey
        }
    }
    return EmojiSkinToneIndex(groups = groups, keyByEmoji = keyByEmoji)
}

fun collapseEmojiSkinToneVariants(
    sourceEmojis: List<BundledEmoji>,
    index: EmojiSkinToneIndex,
): List<EmojiDisplayEntry> {
    val seenGroups = HashSet<EmojiSkinToneGroupKey>()
    val ordered = ArrayList<EmojiDisplayEntry>(sourceEmojis.size)
    for (sourceEmoji in sourceEmojis) {
        val groupKey = index.keyByEmoji[sourceEmoji.emoji]
        if (groupKey == null) {
            ordered += EmojiDisplayEntry(
                emoji = sourceEmoji,
                variants = listOf(sourceEmoji),
                displayEmoji = sourceEmoji.emoji,
            )
        } else if (seenGroups.add(groupKey)) {
            val skinToneGroup = index.groups[groupKey] ?: continue
            ordered += EmojiDisplayEntry(
                emoji = skinToneGroup.base,
                variants = skinToneGroup.variants,
                displayEmoji = sourceEmoji.emoji,
            )
        }
    }
    return ordered
}

private fun buildEmojiSkinToneGroups(allEmojis: List<BundledEmoji>): Map<EmojiSkinToneGroupKey, EmojiSkinToneGroup> {
    val emojisByGroupKey = allEmojis.associateBy { bundledEmoji ->
        EmojiSkinToneGroupKey(
            category = bundledEmoji.category,
            baseName = bundledEmoji.name,
        )
    }
    val skinToneVariantsByGroupKey = linkedMapOf<EmojiSkinToneGroupKey, MutableList<BundledEmoji>>()
    allEmojis.forEach { bundledEmoji ->
        val baseName = skinToneBaseName(bundledEmoji.name) ?: return@forEach
        val groupKey = EmojiSkinToneGroupKey(
            category = bundledEmoji.category,
            baseName = baseName,
        )
        skinToneVariantsByGroupKey
            .getOrPut(groupKey) { mutableListOf() }
            .add(bundledEmoji)
    }
    return skinToneVariantsByGroupKey.mapValues { (groupKey, skinToneVariants) ->
        val baseEmoji = emojisByGroupKey[groupKey] ?: skinToneVariants.first()
        val variants = listOf(baseEmoji).plus(
            skinToneVariants.sortedBy { skinToneVariant ->
                skinToneSortIndex(skinToneVariant.name)
            },
        ).distinctBy { bundledEmoji -> bundledEmoji.emoji }
        EmojiSkinToneGroup(
            base = baseEmoji,
            variants = variants,
        )
    }
}

private fun emojiSkinToneGroupKey(
    emoji: BundledEmoji,
    skinToneGroups: Map<EmojiSkinToneGroupKey, EmojiSkinToneGroup>,
): EmojiSkinToneGroupKey? {
    val baseName = skinToneBaseName(emoji.name) ?: emoji.name
    val groupKey = EmojiSkinToneGroupKey(
        category = emoji.category,
        baseName = baseName,
    )
    return groupKey.takeIf { it in skinToneGroups }
}

private fun skinToneBaseName(name: String): String? {
    val occurrences = emojiSkinToneLabels.sumOf { label ->
        countNonOverlapping(name, ": $label") + countNonOverlapping(name, ", $label")
    }
    if (occurrences != 1) return null
    val matchingLabel = emojiSkinToneLabels
        .filter { label -> name.endsWith(": $label") || name.endsWith(", $label") }
        .maxByOrNull { it.length }
        ?: return null
    return when {
        name.endsWith(": $matchingLabel") -> name.removeSuffix(": $matchingLabel").trim()
        name.endsWith(", $matchingLabel") -> name.removeSuffix(", $matchingLabel").trim()
        else -> null
    }
}

private fun countNonOverlapping(haystack: String, needle: String): Int {
    if (needle.isEmpty()) return 0
    var count = 0
    var index = haystack.indexOf(needle)
    while (index != -1) {
        count++
        index = haystack.indexOf(needle, index + needle.length)
    }
    return count
}

private fun skinToneSortIndex(name: String): Int {
    val matchingIndex = emojiSkinToneLabels
        .withIndex()
        .filter { (_, label) -> name.endsWith(label) }
        .maxByOrNull { (_, label) -> label.length }
        ?.index
    return matchingIndex?.coerceAtLeast(0) ?: 0
}
