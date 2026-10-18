package com.denser.june.core.utils

import com.denser.june.core.domain.model.enums.Fonts

object FontUtils {
    val variableFontNames = setOf(
        Fonts.GOOGLE_SANS_FLEX.fullName,
        Fonts.ROBOTO_FLEX.fullName,
        Fonts.RECURSIVE.fullName,
        Fonts.SHANTELL_SANS.fullName
    )

    fun isVariableFont(fontName: String?): Boolean =
        fontName != null && fontName in variableFontNames

    fun isGoogleSansFlex(fontName: String?): Boolean =
        fontName == Fonts.GOOGLE_SANS_FLEX.fullName

    fun isRobotoFlex(fontName: String?): Boolean =
        fontName == Fonts.ROBOTO_FLEX.fullName

    fun isRecursive(fontName: String?): Boolean =
        fontName == Fonts.RECURSIVE.fullName

    fun isShantellSans(fontName: String?): Boolean =
        fontName == Fonts.SHANTELL_SANS.fullName

    const val SYSTEM_FONT_NAME = "System"

    fun isSystemFont(fontName: String?): Boolean =
        fontName?.equals(SYSTEM_FONT_NAME, ignoreCase = true) == true
}
