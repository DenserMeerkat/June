package com.denser.june.presentation.utils

import android.content.Context
import android.content.res.Configuration
import com.denser.june.core.R
import java.util.Locale

data class PredefinedSecurityQuestion(
    val questionResId: Int,
    val placeholderResId: Int
)

object SecurityQuestionUtils {
    val PREDEFINED_QUESTIONS = listOf(
        PredefinedSecurityQuestion(R.string.question_motto, R.string.placeholder_motto),
        PredefinedSecurityQuestion(R.string.question_book, R.string.placeholder_book),
        PredefinedSecurityQuestion(R.string.question_artist, R.string.placeholder_artist),
        PredefinedSecurityQuestion(R.string.question_writing_place, R.string.placeholder_writing_place),
        PredefinedSecurityQuestion(R.string.question_perfect_day, R.string.placeholder_perfect_day),
        PredefinedSecurityQuestion(R.string.question_first_entry_city, R.string.placeholder_first_entry_city)
    )

    private fun getEnglishString(context: Context, resId: Int): String {
        return try {
            val config = Configuration(context.resources.configuration).apply {
                setLocale(Locale.ENGLISH)
            }
            context.createConfigurationContext(config).resources.getString(resId)
        } catch (_: Exception) {
            context.getString(resId)
        }
    }

    fun findPredefinedQuestionIndex(context: Context, storedQuestion: String?): Int? {
        if (storedQuestion.isNullOrBlank()) return null
        PREDEFINED_QUESTIONS.forEachIndexed { index, item ->
            val currentLocaleString = context.getString(item.questionResId)
            val englishString = getEnglishString(context, item.questionResId)
            if (storedQuestion.equals(currentLocaleString, ignoreCase = true) || 
                storedQuestion.equals(englishString, ignoreCase = true)) {
                return index
            }
        }
        return null
    }

    fun resolveLocalizedQuestion(context: Context, storedQuestion: String): String {
        val index = findPredefinedQuestionIndex(context, storedQuestion)
        return if (index != null) {
            context.getString(PREDEFINED_QUESTIONS[index].questionResId)
        } else {
            storedQuestion
        }
    }
}
