package com.denser.june.core.domain.model.enums

import java.util.concurrent.TimeUnit

enum class AutoBackupInterval(val days: Long) {
    DAILY(1L),
    WEEKLY(7L),
    MONTHLY(30L);

    val repeatIntervalMillis: Long
        get() = TimeUnit.DAYS.toMillis(days)

    companion object {
        fun fromName(name: String?): AutoBackupInterval {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: WEEKLY
        }
    }
}

