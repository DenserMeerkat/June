package com.denser.june.core.data.importers

import com.denser.june.core.domain.importers.ImportSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class FormatSnifferTest {

    @Test
    fun testDetectSyntheticFormats() {
        // Daily You JSON
        val dailyYouJson = """{"mood_scale": 5, "daily-you": {"entries": []}}""".toByteArray()
        assertEquals(ImportSource.DailyYou, FormatSniffer.sniff(ByteArrayInputStream(dailyYouJson), "export.json"))

        // StoryPad JSON
        val storyPadJson = """{"tables": {"stories": []}}""".toByteArray()
        assertEquals(ImportSource.StoryPad, FormatSniffer.sniff(ByteArrayInputStream(storyPadJson), "backup.json"))

        // Day One JSON
        val dayOneJson = """{"entries": [{"uuid": "abc", "creationDate": "2026-01-01"}]}""".toByteArray()
        assertEquals(ImportSource.DayOne, FormatSniffer.sniff(ByteArrayInputStream(dayOneJson), "journal.json"))

        // Diarium by extension (.diary / .diag)
        assertEquals(ImportSource.Diarium, FormatSniffer.sniff(ByteArrayInputStream(byteArrayOf()), "my_journal.diary"))
        assertEquals(ImportSource.Diarium, FormatSniffer.sniff(ByteArrayInputStream(byteArrayOf()), "backup.diag"))

        // Diarium SQLite header
        val sqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        assertEquals(ImportSource.Diarium, FormatSniffer.sniff(ByteArrayInputStream(sqliteHeader), "database.db"))

        // StoryPad by extension (.sp)
        assertEquals(ImportSource.StoryPad, FormatSniffer.sniff(ByteArrayInputStream(byteArrayOf()), "export.sp"))

        // Unknown JSON
        val unknownJson = """{"notes": []}""".toByteArray()
        assertNull(FormatSniffer.sniff(ByteArrayInputStream(unknownJson), "notes.json"))
    }
}
