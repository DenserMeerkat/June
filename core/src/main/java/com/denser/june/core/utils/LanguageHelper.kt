package com.denser.june.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.LocaleConfig
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

data class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String = "",
    val isRtl: Boolean = false
) {
    val displayName: String
        get() = if (englishName.isNotBlank() && !englishName.equals(nativeName, ignoreCase = true)) {
            "$nativeName ($englishName)"
        } else {
            nativeName
        }
}

object LanguageHelper {

    fun openAppLanguageSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Fallback
            }
        }
        return false
    }

    fun getSupportedLanguages(context: Context): List<AppLanguage> {
        val localeTags = mutableSetOf<String>()

        if (Build.VERSION.SDK_INT >= 33) {
            try {
                val localeConfig = LocaleConfig(context)
                val supported = localeConfig.supportedLocales
                if (supported != null) {
                    for (i in 0 until supported.size()) {
                        supported.get(i)?.toLanguageTag()?.let {
                            if (it.isNotBlank()) localeTags.add(it)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to XML parsing
            }
        }

        if (localeTags.isEmpty()) {
            try {
                val xmlId = context.resources.getIdentifier("locales_config", "xml", context.packageName)
                if (xmlId != 0) {
                    val parser = context.resources.getXml(xmlId)
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                            val nameAttr = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                            if (!nameAttr.isNullOrBlank()) {
                                localeTags.add(nameAttr)
                            }
                        }
                        eventType = parser.next()
                    }
                }
            } catch (e: Exception) {
                localeTags.add("en")
            }
        }

        if (localeTags.isEmpty()) {
            localeTags.add("en")
        }

        val languages = localeTags.map { tag ->
            val locale = Locale.forLanguageTag(tag)
            val nativeDisplay = locale.getDisplayName(locale)
            val nativeName = nativeDisplay.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            val englishDisplay = locale.getDisplayName(Locale.ENGLISH)
            val englishName = englishDisplay.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
            val isRtl = when (locale.language.lowercase()) {
                "ar", "fa", "he", "iw", "ur" -> true
                else -> false
            }
            AppLanguage(code = tag, nativeName = nativeName, englishName = englishName, isRtl = isRtl)
        }.sortedBy { it.displayName }

        return listOf(AppLanguage(code = "", nativeName = "System Default", englishName = "")) + languages
    }

    fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) {
            return ""
        }
        val firstLocale: Locale? = locales.get(0)
        return firstLocale?.toLanguageTag() ?: ""
    }

    fun setAppLanguage(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun isCurrentLocaleRtl(): Boolean {
        val locale = Locale.getDefault()
        return when (locale.language.lowercase()) {
            "ar", "fa", "he", "iw", "ur" -> true
            else -> false
        }
    }

    fun getCurrentLanguageNativeName(context: Context): String {
        val currentCode = getCurrentLanguageCode()
        val supported = getSupportedLanguages(context)
        return supported.firstOrNull { it.code.equals(currentCode, ignoreCase = true) }?.nativeName
            ?: supported.first().nativeName
    }
}
