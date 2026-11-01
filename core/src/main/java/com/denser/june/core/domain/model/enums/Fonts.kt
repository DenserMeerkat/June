package com.denser.june.core.domain.model.enums

import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.FontCategory

enum class Fonts(
    val fullName: String,
    val font: Int,
    val category: FontCategory = FontCategory.SANS_SERIF
) {
    GOOGLE_SANS_FLEX("Google Sans Flex", R.font.google_sans_flex, FontCategory.SANS_SERIF),
    ROBOTO_FLEX("Roboto Flex", R.font.roboto_flex, FontCategory.SANS_SERIF),
    RECURSIVE("Recursive", R.font.recursive, FontCategory.SANS_SERIF),
    SHANTELL_SANS("Shantell Sans", R.font.shantell_sans, FontCategory.HANDWRITING)
}