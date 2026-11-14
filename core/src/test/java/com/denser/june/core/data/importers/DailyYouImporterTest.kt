package com.denser.june.core.data.importers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyYouImporterTest {

    @Test
    fun testParseDailyYouV2JsonWithMoodScaleAndTags() {
        val json = """
        {
          "metadata": { "version": 2 },
          "mood_scale": 5,
          "tags": [
            {"id": "tag-1", "name": "Work", "color": "#FF5722"},
            {"id": "tag-2", "name": "Health", "color": "#4CAF50"}
          ],
          "categories": [
            {"id": "cat-1", "name": "Reflections"}
          ],
          "entries": [
            {
              "id": "entry-1",
              "date": "2026-05-10T14:30:00Z",
              "title": "A Great Breakthrough",
              "notes": "Finished the project architecture today!\nEverything fell into place.",
              "mood": 5,
              "category_id": "cat-1",
              "tags": ["tag-1", "tag-2"],
              "attachments": [
                {"id": "att-1", "file_name": "architecture_diagram.png", "type": "image"}
              ]
            },
            {
              "id": "entry-2",
              "date": "2026-05-11T10:00:00Z",
              "notes": "Tough morning commute.",
              "mood": 1
            }
          ]
        }
        """.trimIndent()

        val mediaMap = mapOf(
            "architecture_diagram.png" to "/data/user/0/com.denser.june/files/media/architecture_diagram.png"
        )

        val importer = DailyYouImporter()
        val (journals, _) = importer.parseDailyYouJson(json, mediaMap)

        assertEquals("Should parse 2 entries", 2, journals.size)

        val first = journals[0]
        assertEquals("A Great Breakthrough", first.title)
        assertTrue(first.content.contains("Finished the project architecture today!"))
        // Mood 5 on a 5-point scale maps to 😄
        assertEquals("😄", first.emoji)
        // Space should be Reflections
        assertTrue("Tags should contain Reflections space", first.tags.contains("Reflections"))
        // Topic tags should be normalized with #
        assertTrue("Tags should contain #Work", first.tags.contains("#Work"))
        assertTrue("Tags should contain #Health", first.tags.contains("#Health"))
        // Attached image should be resolved from mediaMap
        assertEquals(1, first.images.size)
        assertEquals("/data/user/0/com.denser.june/files/media/architecture_diagram.png", first.images[0])

        val second = journals[1]
        // Mood 1 on a 5-point scale maps to 😭
        assertEquals("😭", second.emoji)
        assertEquals("Tough morning commute.", second.title)
    }

    @Test
    fun testParseDailyYouV1JsonLegacy() {
        val json = """
        {
          "daily-you": {
            "entries": [
              {
                "id": "v1-entry-1",
                "date": "2026-01-15T08:00:00Z",
                "title": "Morning Routine",
                "content": "Woke up early, meditated and went for a run.",
                "rating": 4,
                "tags": ["morning", "fitness"]
              }
            ]
          }
        }
        """.trimIndent()

        val importer = DailyYouImporter()
        val (journals, _) = importer.parseDailyYouJson(json)

        assertEquals(1, journals.size)
        val journal = journals.first()
        assertEquals("Morning Routine", journal.title)
        assertEquals("🙂", journal.emoji) // Rating 4 maps to 🙂
        assertTrue(journal.tags.contains("#morning"))
        assertTrue(journal.tags.contains("#fitness"))
    }
}
