package com.denser.june.core.data.importers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayOneImporterTest {

    @Test
    fun testParseDayOneSyntheticWithTagsAndAudio() {
        val json = """
        {
          "metadata": { "version": "1.0" },
          "entries": [
            {
              "uuid": "ENTRY-TEST-1",
              "creationDate": "2026-06-01T12:00:00Z",
              "text": "Recorded a song today.\n\n![](dayone-moment://audio-uuid-1)",
              "tags": ["music", "studio"],
              "userActivity": {
                "activityName": "Walking"
              },
              "location": {
                "placeName": "Abbey Road",
                "latitude": 51.5319,
                "longitude": -0.1783
              }
            }
          ]
        }
        """.trimIndent()

        val importer = DayOneImporter()
        val (journals, _) = importer.parseDayOneJson(json, emptyMap(), emptyMap())

        assertEquals(1, journals.size)
        val journal = journals.first()

        assertEquals("Recorded a song today.", journal.title)
        assertTrue(journal.tags.contains("#music"))
        assertTrue(journal.tags.contains("#studio"))
        assertNotNull(journal.location)
        assertEquals("Abbey Road", journal.location?.name)
    }
}
