package com.denser.june.core.data.importers

import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.markdown.HtmlToMarkdown
import com.denser.june.core.domain.model.enums.TagCategory
import com.denser.june.core.utils.MoodEmojiMapper
import com.denser.june.core.utils.TagUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImporterUtilsTest {

    @Test
    fun testHtmlToMarkdown() {
        val html = "<p>Hello <b>world</b>! This is <i>italic</i>.</p><ul><li>First</li><li>Second</li></ul>"
        val md = HtmlToMarkdown.convert(html)
        assertTrue(md.contains("Hello **world**! This is *italic*."))
        assertTrue(md.contains("- First"))
        assertTrue(md.contains("- Second"))
    }

    @Test
    fun testMoodEmojiMapperRating() {
        assertEquals("😭", MoodEmojiMapper.fromRating(1))
        assertEquals("🙁", MoodEmojiMapper.fromRating(2))
        assertEquals("😐", MoodEmojiMapper.fromRating(3))
        assertEquals("🙂", MoodEmojiMapper.fromRating(4))
        assertEquals("😄", MoodEmojiMapper.fromRating(5))
    }

    @Test
    fun testMoodEmojiMapperText() {
        assertEquals("😭", MoodEmojiMapper.fromText("Terrible day"))
        assertEquals("😡", MoodEmojiMapper.fromText("So angry and mad"))
        assertEquals("😴", MoodEmojiMapper.fromText("exhausted"))
        assertEquals("🎯", MoodEmojiMapper.fromText("Very productive today"))
        assertEquals("😊", MoodEmojiMapper.fromText("feeling good"))
    }

    @Test
    fun testMoodEmojiMapperWeather() {
        assertEquals("☀️", MoodEmojiMapper.fromWeather("Clear sky and sunny"))
        assertEquals("🌧️", MoodEmojiMapper.fromWeather("Heavy rain showers"))
        assertEquals("⛅", MoodEmojiMapper.fromWeather("Partly cloudy"))
        assertEquals("❄️", MoodEmojiMapper.fromWeather("Light snow"))
    }

    @Test
    fun testTagNormalizer() {
        // Flat tags become #Topics
        assertEquals("#vacation", TagUtils.normalizeToTopic("vacation"))
        assertEquals("#Work", TagUtils.normalizeToTopic("Work"))

        // @People tags preserved
        assertEquals("@Sarah", TagUtils.normalizeToTopic("@Sarah"))

        // #Topics tags preserved
        assertEquals("#fitness", TagUtils.normalizeToTopic("#fitness"))

        // Spaces category normalized
        assertEquals("Personal", TagUtils.normalizeToSpace("#Personal"))
        assertEquals("Work", TagUtils.normalizeToSpace("Work"))

        // normalizeTags integration
        val normalized = TagUtils.normalizeTags(
            tags = listOf("family", "@Dad", "#gym", "family"),
            categorySpace = "General"
        )
        assertEquals(listOf("General", "#family", "@Dad", "#gym"), normalized)
    }

    @Test
    fun testTagCharacterLimit() {
        val longTag = "ThisIsAnExtremelyLongTagNameToTestLimit"
        val normalized = TagUtils.normalizeToTopic(longTag)
        assertTrue(normalized!!.length <= TagUtils.TAG_CHARACTER_LIMIT)
    }

    @Test
    fun testDeterministicIdGeneration() {
        val id1 = ImporterHelper.generateDeterministicId(ImportSource.DayOne, "UUID-1234")
        val id2 = ImporterHelper.generateDeterministicId(ImportSource.DayOne, "UUID-1234")
        val id3 = ImporterHelper.generateDeterministicId(ImportSource.Diarium, "UUID-1234")
        val id4 = ImporterHelper.generateDeterministicId(ImportSource.DayOne, "UUID-5678")

        assertEquals("Same external ID and source must yield identical deterministic UUID", id1, id2)
        assertNotEquals("Different source must yield different UUID", id1, id3)
        assertNotEquals("Different external ID must yield different UUID", id1, id4)
    }

    @Test
    fun testDiariumTicksToWallClock() {
        // .NET ticks for 2026-01-01T00:00:00Z: 639080736000000000L
        val ticks = 639080736000000000L
        val wallClockMillis = DiariumImporter.diariumTicksToWallClockMillis(ticks)
        assertTrue(wallClockMillis > 0L)
    }

    @Test
    fun testTagDomainOperations() {
        assertEquals(TagCategory.People, TagUtils.getCategoryForTag("@Alice"))
        assertEquals(TagCategory.Topics, TagUtils.getCategoryForTag("#ideas"))
        assertEquals(TagCategory.Spaces, TagUtils.getCategoryForTag("Personal"))

        assertEquals("Alice", TagUtils.getCleanTagName("@Alice"))
        assertEquals("ideas", TagUtils.getCleanTagName("#ideas"))
        assertEquals("Personal", TagUtils.getCleanTagName("Personal"))

        assertEquals("@Alice", TagUtils.appendPrefix("Alice", TagCategory.People))
        assertEquals("#ideas", TagUtils.appendPrefix("ideas", TagCategory.Topics))
        assertEquals("Personal", TagUtils.appendPrefix("Personal", TagCategory.Spaces))

        val mixedTags = listOf("@Alice", "#ideas", "Personal", "@Bob", "#health")
        assertEquals(listOf("@Alice", "@Bob"), TagUtils.filterTagsByCategory(mixedTags, TagCategory.People))
        assertEquals(listOf("#ideas", "#health"), TagUtils.filterTagsByCategory(mixedTags, TagCategory.Topics))
        assertEquals(listOf("Personal"), TagUtils.filterTagsByCategory(mixedTags, TagCategory.Spaces))
    }
}
