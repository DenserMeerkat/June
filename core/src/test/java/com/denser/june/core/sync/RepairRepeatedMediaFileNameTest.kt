package com.denser.june.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairRepeatedMediaFileNameTest {

    private fun cleanRepeatedMediaName(name: String): String {
        return if (name.startsWith("media_") && name.contains("_media_")) {
            "media_" + name.substringAfterLast("_media_")
        } else {
            name
        }
    }

    @Test
    fun testRepeatedMediaNameCleaned() {
        val corrupted = "media_1712345678_media_1712345678_photo.jpg"
        val cleaned = cleanRepeatedMediaName(corrupted)
        assertEquals("media_1712345678_photo.jpg", cleaned)
    }

    @Test
    fun testMultiplyRepeatedMediaNameCleaned() {
        val corrupted = "media_111_media_222_media_333.mp4"
        val cleaned = cleanRepeatedMediaName(corrupted)
        assertEquals("media_333.mp4", cleaned)
    }

    @Test
    fun testNormalMediaNameNotTouched() {
        val normal = "media_1712345678.jpg"
        val cleaned = cleanRepeatedMediaName(normal)
        assertEquals(normal, cleaned)
    }

    @Test
    fun testFileContainingMediaSubstrNotTouched() {
        val normal = "my_special_media_file.jpg"
        val cleaned = cleanRepeatedMediaName(normal)
        assertEquals(normal, cleaned)
    }
}
