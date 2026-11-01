package com.denser.june.core.media.compress

import com.denser.june.core.media.compress.model.CompressionPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCompressorSpecsTest {

    @Test
    fun testGetVideoTargetSpecsForHighRes() {
        val height = 2160 // 4K video

        val low = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.LOW)
        assertEquals(720 to 2_000_000, low)

        val medium = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.MEDIUM)
        assertEquals(1080 to 4_000_000, medium)

        val high = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.HIGH)
        assertEquals(1440 to 8_000_000, high)

        val original = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.ORIGINAL)
        assertEquals(0 to 0, original)
    }

    @Test
    fun testGetVideoTargetSpecsForStandardRes() {
        val height = 1080 // Full HD video

        val low = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.LOW)
        assertEquals(480 to 1_000_000, low)

        val medium = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.MEDIUM)
        assertEquals(720 to 2_000_000, medium)

        val high = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.HIGH)
        assertEquals(1080 to 3_800_000, high)

        val original = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.ORIGINAL)
        assertEquals(0 to 0, original)
    }

    @Test
    fun testGetVideoTargetSpecsForLowRes() {
        val height = 720

        val low = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.LOW)
        assertEquals(480 to 1_000_000, low)

        val medium = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.MEDIUM)
        assertEquals(720 to 2_000_000, medium)

        val high = VideoCompressor.getVideoTargetSpecs(height, CompressionPreset.HIGH)
        assertEquals(1080 to 3_800_000, high)
    }
}
