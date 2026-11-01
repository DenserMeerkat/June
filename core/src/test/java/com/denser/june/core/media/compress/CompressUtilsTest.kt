package com.denser.june.core.media.compress

import com.denser.june.core.media.compress.model.CompressionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompressUtilsTest {

    @Test
    fun testInferPhotoPreset_originalHighRes() {
        // 12MP camera photo (4032 x 3024, ~4.5 MB)
        val result = CompressUtils.inferPhotoPreset(
            width = 4032,
            height = 3024,
            fileSize = 4_500_000L
        )
        assertNull(result)
    }

    @Test
    fun testInferPhotoPreset_uncompressedHighQuality() {
        // Full HD 1080p but high quality/uncompressed PNG/JPEG (1080 x 1920, 3.2 MB)
        val result = CompressUtils.inferPhotoPreset(
            width = 1080,
            height = 1920,
            fileSize = 3_200_000L
        )
        assertNull(result)
    }

    @Test
    fun testInferPhotoPreset_lowCompressed() {
        // 1080p compressed with LOW preset (1080 x 810, ~220 KB, bpp ~0.25)
        val result = CompressUtils.inferPhotoPreset(
            width = 1080,
            height = 810,
            fileSize = 220 * 1024L
        )
        assertEquals(CompressionPreset.LOW, result)
    }

    @Test
    fun testInferPhotoPreset_mediumCompressed() {
        // 1440p compressed with MEDIUM preset (1440 x 1080, ~450 KB, bpp ~0.29)
        val result = CompressUtils.inferPhotoPreset(
            width = 1440,
            height = 1080,
            fileSize = 450 * 1024L
        )
        assertEquals(CompressionPreset.MEDIUM, result)
    }

    @Test
    fun testInferPhotoPreset_highCompressed() {
        // 2048p compressed with HIGH preset (2048 x 1536, ~1.1 MB, bpp ~0.36)
        val result = CompressUtils.inferPhotoPreset(
            width = 2048,
            height = 1536,
            fileSize = 1_100_000L
        )
        assertEquals(CompressionPreset.HIGH, result)
    }

    @Test
    fun testInferPhotoPreset_invalidInputs() {
        assertNull(CompressUtils.inferPhotoPreset(0, 0, 100_000L))
        assertNull(CompressUtils.inferPhotoPreset(1440, 1080, 0L))
        assertNull(CompressUtils.inferPhotoPreset(-1, 1080, 100_000L))
    }

    @Test
    fun testInferVideoPreset_originalHighRes() {
        // 4K video (2160p, 25 Mbps)
        val result = CompressUtils.inferVideoPreset(
            shortEdge = 2160,
            bitrate = 25_000_000
        )
        assertNull(result)
    }

    @Test
    fun testInferVideoPreset_highBitrate1080p() {
        // 1080p video with high bitrate (15 Mbps)
        val result = CompressUtils.inferVideoPreset(
            shortEdge = 1080,
            bitrate = 15_000_000
        )
        assertNull(result)
    }

    @Test
    fun testInferVideoPreset_lowPreset() {
        // 480p video (1 Mbps)
        val result480 = CompressUtils.inferVideoPreset(
            shortEdge = 480,
            bitrate = 1_000_000
        )
        assertEquals(CompressionPreset.LOW, result480)

        // 720p video transcoded at low bitrate (1.4 Mbps)
        val result720Low = CompressUtils.inferVideoPreset(
            shortEdge = 720,
            bitrate = 1_400_000
        )
        assertEquals(CompressionPreset.LOW, result720Low)
    }

    @Test
    fun testInferVideoPreset_mediumPreset() {
        // 720p video at standard medium bitrate (2.5 Mbps)
        val result720 = CompressUtils.inferVideoPreset(
            shortEdge = 720,
            bitrate = 2_500_000
        )
        assertEquals(CompressionPreset.MEDIUM, result720)

        // 1080p video transcoded down to medium bitrate (3.2 Mbps)
        val result1080Med = CompressUtils.inferVideoPreset(
            shortEdge = 1080,
            bitrate = 3_200_000
        )
        assertEquals(CompressionPreset.MEDIUM, result1080Med)
    }

    @Test
    fun testInferVideoPreset_highPreset() {
        // 1080p video at high preset bitrate (5.0 Mbps)
        val result1080 = CompressUtils.inferVideoPreset(
            shortEdge = 1080,
            bitrate = 5_000_000
        )
        assertEquals(CompressionPreset.HIGH, result1080)

        // 1440p video at high preset bitrate (6.0 Mbps)
        val result1440 = CompressUtils.inferVideoPreset(
            shortEdge = 1440,
            bitrate = 6_000_000
        )
        assertEquals(CompressionPreset.HIGH, result1440)
    }

    @Test
    fun testInferVideoPreset_invalidInputs() {
        assertNull(CompressUtils.inferVideoPreset(0, 5_000_000))
        assertNull(CompressUtils.inferVideoPreset(-100, 2_000_000))
    }
}
