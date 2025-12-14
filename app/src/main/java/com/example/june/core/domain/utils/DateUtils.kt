package com.example.june.core.domain.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.toDateWithDay(): String {
    val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDateWithDay(): String {
    val sdf = SimpleDateFormat("EEE, MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDate(): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

data class TimeOptions(
    val useRelativeTime: Boolean = false,
    val maxRelativeThreshold: Long = 24 * 60 * 60 * 1000L,

    val dateFormatSameDay: String = "HH:mm",
    val dateFormatSameWeek: String = "EEE",
    val dateFormatDefault: String = "dd/MM/yyyy",
    val dateFormatSameYear: String? = null,

    val labels: RelativeTimeLabels = RelativeTimeLabels(),
    val locale: Locale = Locale.getDefault()
)

data class RelativeTimeLabels(
    val justNow: String = "Just now",
    val minAgo: String = "min ago",
    val minsAgo: String = "mins ago",
    val hrAgo: String = "hr ago",
    val hrsAgo: String = "hrs ago",
    val yesterday: String = "Yesterday"
)

fun formatTimestamp(
    timestamp: Long,
    options: TimeOptions = TimeOptions()
): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    if (diff < 0) {
        return SimpleDateFormat(options.dateFormatDefault, options.locale).format(Date(timestamp))
    }

    if (options.useRelativeTime && diff < options.maxRelativeThreshold) {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            seconds < 60 -> options.labels.justNow
            minutes < 60 -> "$minutes ${if (minutes == 1L) options.labels.minAgo else options.labels.minsAgo}"
            hours < 24 -> "$hours ${if (hours == 1L) options.labels.hrAgo else options.labels.hrsAgo}"
            else -> options.labels.yesterday // Should only be reached if threshold is > 24h
        }
    }

    val oneDayMillis = 24 * 60 * 60 * 1000L
    val oneWeekMillis = 7 * oneDayMillis

    val isSameYear = if (options.dateFormatSameYear != null) {
        val calendarYear = SimpleDateFormat("yyyy", options.locale).format(Date(now))
        val timestampYear = SimpleDateFormat("yyyy", options.locale).format(Date(timestamp))
        calendarYear == timestampYear
    } else false

    return when {
        diff < oneDayMillis -> {
            SimpleDateFormat(options.dateFormatSameDay, options.locale).format(Date(timestamp))
        }
        diff < oneWeekMillis -> {
            SimpleDateFormat(options.dateFormatSameWeek, options.locale).format(Date(timestamp))
        }
        isSameYear -> {
            SimpleDateFormat(options.dateFormatSameYear!!, options.locale).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat(options.dateFormatDefault, options.locale).format(Date(timestamp))
        }
    }
}

fun formatDateTime(timestamp: Long): String {
    return formatTimestamp(timestamp, options = TimeOptions())
}

fun formatTime(timestamp: Long): String {
    return formatTimestamp(
        timestamp,
        options = TimeOptions(
            dateFormatSameDay = "HH:mm",
            dateFormatSameWeek = "HH:mm",
            dateFormatDefault = "HH:mm"
        )
    )
}