package com.denser.june.core.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalAudioTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Test
    fun testJournalAudioSerialization() {
        val audio = JournalAudio(
            id = "audio-123",
            path = "/data/user/0/com.denser.june/files/journal_audio/rec_1.m4a",
            durationMs = 45000L,
            title = "Morning Voice Note",
            format = "m4a",
            size = 102400L,
            waveform = listOf(0.1f, 0.5f, 0.8f, 0.2f),
            createdAt = 1700000000000L
        )

        val encoded = json.encodeToString(JournalAudio.serializer(), audio)
        val decoded = json.decodeFromString(JournalAudio.serializer(), encoded)

        assertEquals(audio.id, decoded.id)
        assertEquals(audio.path, decoded.path)
        assertEquals(audio.durationMs, decoded.durationMs)
        assertEquals(audio.title, decoded.title)
        assertEquals(audio.format, decoded.format)
        assertEquals(audio.size, decoded.size)
        assertEquals(audio.waveform, decoded.waveform)
        assertEquals(audio.createdAt, decoded.createdAt)
    }

    @Test
    fun testJournalContentHashIgnoresAudioPathPrefix() {
        val audio1 = JournalAudio(
            id = "aud-1",
            path = "/data/user/0/com.denser.june/files/journal_audio/test_audio.m4a",
            durationMs = 30000L,
            title = "Note"
        )
        val audio2 = JournalAudio(
            id = "aud-1",
            path = "/storage/emulated/0/Android/data/com.denser.june/files/journal_audio/test_audio.m4a",
            durationMs = 30000L,
            title = "Note"
        )

        val journal1 = Journal(
            id = "j-1",
            title = "Entry",
            content = "Testing audio",
            createdAt = 1000L,
            updatedAt = 2000L,
            dateTime = 1000L,
            audios = listOf(audio1)
        )

        val journal2 = Journal(
            id = "j-1",
            title = "Entry",
            content = "Testing audio",
            createdAt = 1000L,
            updatedAt = 2000L,
            dateTime = 1000L,
            audios = listOf(audio2)
        )

        assertTrue(journal1.isContentEqualTo(journal2))
        assertEquals(journal1.computeContentHash(), journal2.computeContentHash())
    }
}

