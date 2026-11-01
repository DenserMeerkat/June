package com.denser.june.core.media.compress

import com.denser.june.core.media.compress.model.CompressionPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class CompressionPresetTest {

    @Test
    fun testFromStepIndex() {
        assertEquals(CompressionPreset.LOW, CompressionPreset.fromStepIndex(0))
        assertEquals(CompressionPreset.MEDIUM, CompressionPreset.fromStepIndex(1))
        assertEquals(CompressionPreset.HIGH, CompressionPreset.fromStepIndex(2))
        assertEquals(CompressionPreset.ORIGINAL, CompressionPreset.fromStepIndex(3))
    }

    @Test
    fun testFromStepIndexFallback() {
        assertEquals(CompressionPreset.MEDIUM, CompressionPreset.fromStepIndex(-5))
        assertEquals(CompressionPreset.MEDIUM, CompressionPreset.fromStepIndex(10))
    }

    @Test
    fun testStepIndexProperty() {
        assertEquals(0, CompressionPreset.LOW.stepIndex)
        assertEquals(1, CompressionPreset.MEDIUM.stepIndex)
        assertEquals(2, CompressionPreset.HIGH.stepIndex)
        assertEquals(3, CompressionPreset.ORIGINAL.stepIndex)
    }

    @Test
    fun testLabels() {
        assertEquals("Original", CompressionPreset.ORIGINAL.label)
        assertEquals("High", CompressionPreset.HIGH.label)
        assertEquals("Medium", CompressionPreset.MEDIUM.label)
        assertEquals("Low", CompressionPreset.LOW.label)
    }
}
