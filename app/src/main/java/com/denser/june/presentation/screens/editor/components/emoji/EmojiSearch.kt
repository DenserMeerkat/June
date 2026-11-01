// Adapted from https://github.com/bikram-agarwal/Remember/blob/main/app/src/main/java/dev/bikram/remember/ui/edit/IconPickerSearch.kt

package com.denser.june.presentation.screens.editor.components.emoji

import java.util.Locale
import kotlin.math.abs

internal data class SearchableField(
    val text: String,
    val weight: Float,
    val prefixMatchEnabled: Boolean = true,
)

private const val EXACT_WORD_SCORE = 1.0f
private const val STEMMED_WORD_SCORE = 0.85f
private const val WORD_PREFIX_SCORE = 0.75f
private const val FUZZY_ONE_EDIT_SCORE = 0.5f
private const val FUZZY_MIN_TOKEN_LENGTH = 5

private const val FIELD_WEIGHT_NAME: Float = 3.0f
private const val FIELD_WEIGHT_SLUG: Float = 2.0f
private const val FIELD_WEIGHT_KEYWORDS: Float = 1.5f
private const val FIELD_WEIGHT_CATEGORY: Float = 0.8f

private val WHITESPACE_SPLIT = Regex("\\s+")
private val FIELD_WORD_SPLIT = Regex("[\\s_\\-]+")

internal fun tokenizeQuery(rawQuery: String): List<String> =
    rawQuery
        .lowercase(Locale.getDefault())
        .split(WHITESPACE_SPLIT)
        .filter { it.isNotEmpty() }

internal data class EmojiSearchResult(
    val emoji: BundledEmoji,
    val matchedTokensCount: Int,
    val score: Float,
)

fun searchAndRankEmojis(
    emojis: List<BundledEmoji>,
    selectedCategoryKey: String,
    rawQuery: String,
): List<BundledEmoji> {
    val tokens = tokenizeQuery(rawQuery)
    if (tokens.isEmpty()) {
        return emojis.filter { it.category == selectedCategoryKey }
    }

    val results = ArrayList<EmojiSearchResult>(emojis.size)
    for (emoji in emojis) {
        val fields = buildEmojiSearchFields(emoji)
        var totalScore = 0f
        var matchedCount = 0

        for (token in tokens) {
            val tokenStem = lightStem(token)
            var bestForToken = 0f
            for (field in fields) {
                val candidate = scoreFieldForToken(token, tokenStem, field)
                if (candidate > bestForToken) {
                    bestForToken = candidate
                }
            }
            if (bestForToken > 0f) {
                matchedCount++
                totalScore += bestForToken
            }
        }

        if (matchedCount > 0) {
            results.add(
                EmojiSearchResult(
                    emoji = emoji,
                    matchedTokensCount = matchedCount,
                    score = totalScore,
                )
            )
        }
    }

    val totalTokens = tokens.size
    return results
        .sortedWith(
            compareByDescending<EmojiSearchResult> { it.matchedTokensCount == totalTokens } // Tier 1 (Strict AND)
                .thenByDescending { it.matchedTokensCount } // Tier 2 (More matching tokens first)
                .thenByDescending { it.score }              // Higher weighted relevance
                .thenBy { it.emoji.name.length }           // Shorter name
                .thenBy { it.emoji.name.lowercase(Locale.getDefault()) }
        )
        .map { it.emoji }
}

private fun buildEmojiSearchFields(emoji: BundledEmoji): List<SearchableField> {
    val categoryLabel = emoji.category.replace('_', ' ')
    return listOf(
        SearchableField(text = emoji.name, weight = FIELD_WEIGHT_NAME),
        SearchableField(text = emoji.slug.replace('_', ' '), weight = FIELD_WEIGHT_SLUG),
        SearchableField(text = emoji.keywords.joinToString(" "), weight = FIELD_WEIGHT_KEYWORDS, prefixMatchEnabled = false),
        SearchableField(text = categoryLabel, weight = FIELD_WEIGHT_CATEGORY, prefixMatchEnabled = false),
    )
}

private fun scoreFieldForToken(
    token: String,
    tokenStem: String,
    field: SearchableField,
): Float {
    val lower = field.text.lowercase(Locale.getDefault())
    if (lower.isEmpty()) return 0f
    val words = lower.split(FIELD_WORD_SPLIT)
    val fuzzyEligible = token.length >= FUZZY_MIN_TOKEN_LENGTH
    var bestScore = 0f

    for (word in words) {
        if (word.isEmpty()) continue
        if (word == token) {
            return field.weight * EXACT_WORD_SCORE + token.length * 0.02f
        }
        if (tokenStem != token && (word == tokenStem || lightStem(word) == tokenStem)) {
            val candidate = field.weight * STEMMED_WORD_SCORE + tokenStem.length * 0.02f
            if (candidate > bestScore) bestScore = candidate
            continue
        }
        if (!field.prefixMatchEnabled) continue
        if (word.length > token.length && word.startsWith(token)) {
            val candidate = field.weight * WORD_PREFIX_SCORE + token.length * 0.01f
            if (candidate > bestScore) bestScore = candidate
            continue
        }
        if (fuzzyEligible && isWithinOneEdit(word, token)) {
            val candidate = field.weight * FUZZY_ONE_EDIT_SCORE
            if (candidate > bestScore) bestScore = candidate
        }
    }
    return bestScore
}

internal fun lightStem(token: String): String {
    val length = token.length
    if (length < 4) return token
    if (length >= 5 && token.endsWith("ies")) return token.substring(0, length - 3) + "y"
    if (length >= 5 && token.endsWith("ing")) return collapseDoubledConsonant(token.substring(0, length - 3))
    if (length >= 5 && token.endsWith("es")) return token.substring(0, length - 2)
    if (length >= 4 && token.endsWith("ed")) return collapseDoubledConsonant(token.substring(0, length - 2))
    if (length >= 4 && token.endsWith("s") && !token.endsWith("ss")) return token.substring(0, length - 1)
    return token
}

private fun collapseDoubledConsonant(stem: String): String {
    if (stem.length < 3) return stem
    val last = stem[stem.length - 1]
    val secondLast = stem[stem.length - 2]
    if (last != secondLast) return stem
    if (last == 'l' || last == 's' || last == 'z') return stem
    return stem.substring(0, stem.length - 1)
}

internal fun isWithinOneEdit(a: String, b: String): Boolean {
    val la = a.length
    val lb = b.length
    if (la == lb) {
        var diffs = 0
        var index = 0
        while (index < la) {
            if (a[index] != b[index]) {
                diffs++
                if (diffs > 1) return false
            }
            index++
        }
        return true
    }
    if (abs(la - lb) != 1) return false
    val (shorter, longer) = if (la < lb) a to b else b to a
    var indexShort = 0
    var indexLong = 0
    var seenSkip = false
    while (indexShort < shorter.length && indexLong < longer.length) {
        if (shorter[indexShort] != longer[indexLong]) {
            if (seenSkip) return false
            seenSkip = true
            indexLong++
        } else {
            indexShort++
            indexLong++
        }
    }
    return true
}
