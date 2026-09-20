package com.denser.june.core.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongDetailsAndJournalTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun testSongDetailsBackwardCompatibility() {
        val legacyJson = """
            {
                "title": "Midnight City",
                "artistName": "M83",
                "thumbnailUrl": "https://example.com/art.jpg",
                "previewUrl": "https://example.com/preview.mp3",
                "previewUrlProvider": "Spotify"
            }
        """.trimIndent()

        val details = json.decodeFromString<SongDetails>(legacyJson)
        assertEquals("Midnight City", details.title)
        assertEquals("M83", details.artistName)
        assertEquals(0L, details.clipStartMs)
        assertEquals(null, details.clipEndMs)
        assertEquals(SongSourceType.LINK, details.sourceType)
        assertEquals(null, details.localPreviewPath)
        assertEquals(null, details.localThumbnailPath)
    }

    @Test
    fun testJournalContentHashIgnoresDeviceAbsoluteSongPaths() {
        val song1 = SongDetails(
            title = "Midnight City",
            artistName = "M83",
            localPreviewPath = "/data/user/0/com.denser.june/files/song_media/library/abc12345.mp3",
            localThumbnailPath = "/data/user/0/com.denser.june/files/song_media/art/art12345.jpg"
        )
        val song2 = SongDetails(
            title = "Midnight City",
            artistName = "M83",
            localPreviewPath = "/storage/emulated/0/Android/data/com.denser.june/files/song_media/library/abc12345.mp3",
            localThumbnailPath = "/storage/emulated/0/Android/data/com.denser.june/files/song_media/art/art12345.jpg"
        )

        val journal1 = Journal(
            id = "j-1",
            title = "My Entry",
            content = "Hello world",
            createdAt = 1000L,
            updatedAt = 2000L,
            dateTime = 1000L,
            songDetails = song1
        )

        val journal2 = Journal(
            id = "j-1",
            title = "My Entry",
            content = "Hello world",
            createdAt = 1000L,
            updatedAt = 2000L,
            dateTime = 1000L,
            songDetails = song2
        )

        assertEquals(journal1.computeContentHash(), journal2.computeContentHash())
        assertTrue(journal1.isContentEqualTo(journal2))
    }
}
