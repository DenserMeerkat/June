package com.denser.june.core.domain.utils

import java.time.YearMonth
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.time.format.DateTimeFormatter

fun Long.toYearMonth(): YearMonth {
    val localDate = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return YearMonth.from(localDate)
}

fun Long.toShortMonth(): String {
    val sdf = SimpleDateFormat("MMM", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDayOfMonth(): String {
    val sdf = SimpleDateFormat("dd", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDateWithDay(): String {
    val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFullDate(): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toHoursMinutesSeconds(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

fun YearMonth.getDaysInMonthGrid(): List<LocalDate?> {
    val firstDay = this.atDay(1)
    val totalDays = this.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value % 7
    val list = mutableListOf<LocalDate?>()
    repeat(startOffset) { list.add(null) }
    for (i in 1..totalDays) {
        list.add(this.atDay(i))
    }
    return list
}

fun Long.isMidnight(): Boolean {
    return this.toLocalTime() == LocalTime.MIDNIGHT
}

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun Long.toLocalTime(): LocalTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalTime()

fun LocalDate.toFullDate(): String =
    this.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()))

fun LocalTime.toFullTime(): String =
    this.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))

fun combineDateAndTime(date: LocalDate, time: LocalTime?): Long {
    val dateTime = if (time != null) {
        LocalDateTime.of(date, time)
    } else {
        date.atStartOfDay()
    }
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun getTodayAtMidnight(): Long {
    return LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

