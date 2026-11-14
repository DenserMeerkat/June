package com.denser.june.core.data.importers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DiariumImporterTest {

    @Test
    fun testDiariumTicksToWallClockMillis() {
        // Ticks for 2026-01-01T00:00:00 UTC
        val ldt = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
        val epochSeconds = ldt.toEpochSecond(ZoneOffset.UTC)
        val ticks = (epochSeconds + 62135596800L) * 10_000_000L

        val converted = DiariumImporter.diariumTicksToWallClockMillis(ticks)
        val convertedLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(converted), ZoneId.systemDefault())
        assertEquals(2026, convertedLdt.year)
        assertEquals(1, convertedLdt.monthValue)
        assertEquals(1, convertedLdt.dayOfMonth)
        assertEquals(0, convertedLdt.hour)
        assertEquals(0, convertedLdt.minute)
    }

    @Test
    fun testDiariumRecentDateTicks() {
        // Ticks for 2026-11-13T19:55:00
        val ldt = LocalDateTime.of(2026, 11, 13, 19, 55, 0)
        val epochSeconds = ldt.toEpochSecond(ZoneOffset.UTC)
        val ticks = (epochSeconds + 62135596800L) * 10_000_000L

        val converted = DiariumImporter.diariumTicksToWallClockMillis(ticks)
        val convertedLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(converted), ZoneId.systemDefault())
        assertEquals(2026, convertedLdt.year)
        assertEquals(11, convertedLdt.monthValue)
        assertEquals(13, convertedLdt.dayOfMonth)
        assertEquals(19, convertedLdt.hour)
        assertEquals(55, convertedLdt.minute)
    }
}
