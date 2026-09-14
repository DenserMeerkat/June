package com.denser.june.core.markdown

import com.denser.june.core.domain.markdown.MarkdownEngine
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.SongDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownEngineTest {

    @Test
    fun testSerializationRoundTrip() {
        val original = Journal(
            id = "test-journal-123",
            title = "A Beautiful Day",
            content = "This is a great day with lots of sunshine.\nMultiple lines of thoughts.",
            emoji = "☀️",
            images = listOf("image1.jpg", "image2.png"),
            location = JournalLocation(
                latitude = 37.7749,
                longitude = -122.4194,
                address = "San Francisco, CA",
                name = "Golden Gate Park",
                locality = "San Francisco"
            ),
            songDetails = SongDetails(
                title = "Sunny Days",
                artistName = "The Sunshines",
                thumbnailUrl = "https://example.com/thumb.jpg",
                previewUrl = "https://example.com/preview.mp3",
                previewUrlProvider = "Spotify",
                links = com.denser.june.core.domain.model.PlatformLinks()
            ),
            tags = listOf("sunshine", "nature", "walk"),
            createdAt = 1773000000000L,
            updatedAt = 1773010000000L,
            dateTime = 1773000000000L,
            isBookmarked = true,
            isArchived = false,
            isDraft = false
        )

        val markdown = MarkdownEngine.toMarkdown(original)
        val reconstructed = MarkdownEngine.fromMarkdown(markdown, fallbackTitle = "Fallback")

        assertEquals(original.id, reconstructed.id)
        assertEquals(original.title, reconstructed.title)
        assertEquals(original.content, reconstructed.content)
        assertEquals(original.emoji, reconstructed.emoji)
        assertEquals(original.isBookmarked, reconstructed.isBookmarked)
        assertEquals(original.isArchived, reconstructed.isArchived)
        assertEquals(original.tags, reconstructed.tags)
        assertEquals(original.location?.name, reconstructed.location?.name)
        assertEquals(original.location?.address, reconstructed.location?.address)
        assertEquals(original.songDetails?.title, reconstructed.songDetails?.title)
        assertEquals(original.songDetails?.artistName, reconstructed.songDetails?.artistName)
    }

    @Test
    fun testRawMarkdownWithoutFrontmatter_ExtractsHeaderAsTitle() {
        val raw = """
            # Meeting with the team
            
            We discussed the upcoming roadmap and agreed on priority tasks.
            - Item 1
            - Item 2
        """.trimIndent()

        val parsed = MarkdownEngine.fromMarkdown(raw, fallbackTitle = "Fallback Title")

        assertEquals("Meeting with the team", parsed.title)
        assertEquals("We discussed the upcoming roadmap and agreed on priority tasks.\n- Item 1\n- Item 2", parsed.content)
    }

    @Test
    fun testRawMarkdownWithoutFrontmatterOrHeader_UsesFallbackTitle() {
        val raw = "Just a quick note about grocery list:\nMilk\nEggs\nBread"

        val parsed = MarkdownEngine.fromMarkdown(raw, fallbackTitle = "2026-09-12_Grocery_List.md")

        assertEquals("Grocery List", parsed.title)
        assertEquals(raw, parsed.content)
    }

    @Test
    fun testObsidianStyleFrontmatter() {
        val raw = """
            ---
            title: "Trip to Kyoto"
            date: "2026-04-15"
            tags: [travel, japan, vacation]
            emoji: "🗾"
            ---
            
            # Trip to Kyoto
            
            Visiting Fushimi Inari Shrine and Kinkaku-ji.
        """.trimIndent()

        val parsed = MarkdownEngine.fromMarkdown(raw)

        assertEquals("Trip to Kyoto", parsed.title)
        assertEquals("Visiting Fushimi Inari Shrine and Kinkaku-ji.", parsed.content)
        assertEquals("🗾", parsed.emoji)
        assertEquals(listOf("travel", "japan", "vacation"), parsed.tags)
    }

    @Test
    fun testGenerateFileName() {
        val journal = Journal(
            id = "abc-123",
            title = "My Summer Vacation: Day 1/2",
            content = "Hello",
            createdAt = 1773000000000L,
            updatedAt = null,
            dateTime = 1773000000000L
        )

        val batchFileName = MarkdownEngine.generateFileName(journal, forSingleExport = false)
        assertFalse(batchFileName.contains(":"))
        assertFalse(batchFileName.contains("/"))
        assertFalse(batchFileName.contains("abc-123")) // Should NOT leak raw UUID in filename
        assertTrue(batchFileName.endsWith("My_Summer_Vacation_Day_12.md"))

        val singleFileName = MarkdownEngine.generateFileName(journal, forSingleExport = true)
        assertEquals("My_Summer_Vacation_Day_12.md", singleFileName)

        val untitledJournal = journal.copy(title = "", content = "")
        val untitledFileName = MarkdownEngine.generateFileName(untitledJournal)
        assertFalse(untitledFileName.contains("abc-123"))
        assertTrue(untitledFileName.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{4}\\.md$".toRegex()))
    }

    @Test
    fun testChatbotExportMarkdown() {
        val raw = """
            # Guide to Machine Learning
            
            Machine learning is a subset of AI.
            
            ## Key Concepts
            - Supervised Learning
            - Unsupervised Learning
            
            ```python
            print("Hello ML")
            ```
        """.trimIndent()

        val parsed = MarkdownEngine.fromMarkdown(raw, fallbackTitle = "export.md")
        assertEquals("Guide to Machine Learning", parsed.title)
        assertTrue(parsed.content.contains("Machine learning is a subset of AI."))
        assertTrue(parsed.content.contains("## Key Concepts"))
        assertTrue(parsed.content.contains("```python"))
    }

    @Test
    fun testFrontmatterWithUnknownKeys_DoesNotThrow() {
        val raw = """
            ---
            id: "obsidian-note-999"
            title: "Advanced Obsidian Note"
            author: "Alice Researcher"
            status: "published"
            aliases: ["AI Research", "Machine Intelligence"]
            cssclasses: ["wide-page", "dark-mode"]
            date: "2026-09-12"
            tags: ["research", "ai"]
            ---
            
            # Advanced Obsidian Note
            
            Here is the body of the note with third-party frontmatter fields.
        """.trimIndent()

        val parsed = MarkdownEngine.fromMarkdown(raw)
        assertEquals("obsidian-note-999", parsed.id)
        assertEquals("Advanced Obsidian Note", parsed.title)
        assertEquals(listOf("research", "ai"), parsed.tags)
        assertEquals("Here is the body of the note with third-party frontmatter fields.", parsed.content)
    }

    @Test
    fun testObsidianWikilinkMediaExtraction() {
        val raw = """
            ---
            title: "Trip Photo Journal"
            ---
            
            Here is our hotel:
            ![[hotel_room.jpg]]
            
            And here is standard markdown image:
            ![Beach](beach_sunset.png)
        """.trimIndent()

        val parsed = MarkdownEngine.fromMarkdown(raw)
        assertEquals("Trip Photo Journal", parsed.title)
        assertEquals(listOf("hotel_room.jpg", "beach_sunset.png"), parsed.images)
    }

    @Test
    fun testExportMarkdown_IncludeMediaToggle() {
        val journal = Journal(
            id = "test-media-toggle",
            title = "Photo Trip",
            content = "Walking in the woods.",
            images = listOf("forest.jpg", "mountain.png"),
            createdAt = 1773000000000L,
            updatedAt = null,
            dateTime = 1773000000000L
        )

        val mdWithMedia = MarkdownEngine.toMarkdown(journal, relativeMediaPathPrefix = "media", includeMedia = true)
        assertTrue(mdWithMedia.contains("## Media"))
        assertTrue(mdWithMedia.contains("media/forest.jpg"))
        assertTrue(mdWithMedia.contains("media/mountain.png"))

        val mdWithoutMedia = MarkdownEngine.toMarkdown(journal, relativeMediaPathPrefix = "media", includeMedia = false)
        assertFalse(mdWithoutMedia.contains("## Media"))
        assertFalse(mdWithoutMedia.contains("media/forest.jpg"))
        assertFalse(mdWithoutMedia.contains("media/mountain.png"))
        assertTrue(mdWithoutMedia.contains("Walking in the woods."))
    }

    @Test
    fun testExportMarkdown_HumanizedSubtitleAndRoundtrip() {
        val journal = Journal(
            id = "test-subtitle",
            title = "Concert Night",
            content = "Great music and atmosphere!",
            location = JournalLocation(
                latitude = 40.7505,
                longitude = -73.9934,
                name = "Madison Square Garden",
                address = "New York, NY"
            ),
            songDetails = SongDetails(title = "Fix You", artistName = "Coldplay"),
            createdAt = 1773000000000L,
            updatedAt = null,
            dateTime = 1773000000000L
        )

        val md = MarkdownEngine.toMarkdown(journal)
        assertTrue(md.contains("📍 Madison Square Garden"))
        assertTrue(md.contains("🎵 Fix You — Coldplay"))

        val parsed = MarkdownEngine.fromMarkdown(md)
        assertEquals("Concert Night", parsed.title)
        assertEquals("Madison Square Garden", parsed.location?.name)
        assertEquals("Fix You", parsed.songDetails?.title)
        assertEquals("Great music and atmosphere!", parsed.content)
    }
}
