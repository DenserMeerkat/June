package com.denser.june.core.domain.utils

import android.util.Patterns

fun String.isValidLink(): Boolean {
    return this.isNotBlank() && Patterns.WEB_URL.matcher(this.trim()).matches()
}