package com.denser.june.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatBytesTest {

    @Test
    fun testZeroBytes() {
        assertEquals("0 B", 0L.formatBytes())
        assertEquals("0 B", (-100L).formatBytes())
    }

    @Test
    fun testKilobytes() {
        val result = 1024L.formatBytes()
        assertTrue(result.contains("KB"))
        assertTrue(result.startsWith("1.0"))
    }

    @Test
    fun testMegabytes() {
        val result = (5 * 1024 * 1024L).formatBytes()
        assertTrue(result.contains("MB"))
        assertTrue(result.startsWith("5.0"))
    }

    @Test
    fun testGigabytes() {
        val result = (2L * 1024 * 1024 * 1024).formatBytes()
        assertTrue(result.contains("GB"))
        assertTrue(result.startsWith("2.0"))
    }
}
