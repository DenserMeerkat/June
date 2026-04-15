package com.denser.june.core.utils

import java.time.YearMonth
import java.time.DayOfWeek
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

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

fun Long.toFullDateTime(): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
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

fun YearMonth.getDaysInMonthGrid(startOfWeek: DayOfWeek = DayOfWeek.SUNDAY): List<LocalDate?> {
    val firstDay = this.atDay(1)
    val totalDays = this.lengthOfMonth()
    val firstDayOfWeek = firstDay.dayOfWeek.value
    val startDayValue = startOfWeek.value

    val startOffset = (firstDayOfWeek - startDayValue + 7) % 7
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

fun getWeeksInMonth(yearMonth: YearMonth): Int {
    val firstDay = yearMonth.atDay(1)
    val lastDay = yearMonth.atEndOfMonth()
    val weekFields = WeekFields.of(Locale.getDefault())
    val weekOne = firstDay.get(weekFields.weekOfWeekBasedYear())
    var weekLast = lastDay.get(weekFields.weekOfWeekBasedYear())
    if (weekLast < weekOne) {
        weekLast += 52
    }
    return (weekLast - weekOne) + 1
}

