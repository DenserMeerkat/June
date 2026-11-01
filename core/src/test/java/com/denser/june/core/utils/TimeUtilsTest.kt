package com.denser.june.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.YearMonth

class TimeUtilsTest {

    @Test
    fun getWeeksInMonth_returnsExactGridRowCount() {
        assertEquals(4, getWeeksInMonth(YearMonth.of(2021, 2), DayOfWeek.MONDAY))
        assertEquals(5, getWeeksInMonth(YearMonth.of(2021, 2), DayOfWeek.SUNDAY))

        assertEquals(6, getWeeksInMonth(YearMonth.of(2026, 5), DayOfWeek.SUNDAY))
        assertEquals(5, getWeeksInMonth(YearMonth.of(2026, 5), DayOfWeek.MONDAY))

        assertEquals(5, getWeeksInMonth(YearMonth.of(2026, 11), DayOfWeek.SUNDAY))
        assertEquals(6, getWeeksInMonth(YearMonth.of(2026, 11), DayOfWeek.MONDAY))

        assertEquals(6, getWeeksInMonth(YearMonth.of(2026, 8), DayOfWeek.SUNDAY))
        assertEquals(6, getWeeksInMonth(YearMonth.of(2026, 8), DayOfWeek.MONDAY))
    }
}

