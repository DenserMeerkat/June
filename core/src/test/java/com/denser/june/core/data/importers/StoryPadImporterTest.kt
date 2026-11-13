package com.denser.june.core.data.importers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryPadImporterTest {

    @Test
    fun testParseStoryPadV2WithDataRootAndColorSeed() {
        val json = """
        {
          "version": 2,
          "data": {
            "tags": [
              {"id": 42, "title": "Travel", "emoji": "✈️"}
            ],
            "stories": [
              {
                "id": "1001",
                "year": 2026,
                "month": 6,
                "day": 15,
                "hour": 10,
                "minute": 30,
                "starred": true,
                "tags": [42],
                "preferences": {
                  "color_seed_value": -16711936,
                  "font_family": "Roboto"
                },
                "latest_content": {
                  "title": "Summer Trip",
                  "plain_text": "Flight was on time and smooth."
                }
              }
            ]
          }
        }
        """.trimIndent()

        val importer = StoryPadImporter()
        val journals = importer.parseStoryPadJson(json)

        assertEquals(1, journals.size)
        val journal = journals.first()
        assertEquals("Summer Trip", journal.title)
        assertEquals("Flight was on time and smooth.", journal.content)
        assertTrue(journal.isBookmarked)
        assertEquals("✈️", journal.emoji)
        assertTrue(journal.tags.contains("Travel") || journal.tags.contains("#Travel"))
        assertEquals(-16711936, journal.theme?.primaryColor)
        assertEquals("Roboto", journal.font)
    }
}
