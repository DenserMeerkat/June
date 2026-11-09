package com.denser.june.core.sync

import com.denser.june.core.domain.sync.SyncHashCache
import com.denser.june.core.utils.computeSHA256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SyncHashCacheTest {

    private lateinit var tempDir: File
    private lateinit var syncCacheDir: File
    private lateinit var cache: SyncHashCache

    @Before
    fun setUp() {
        tempDir = createTempDirectory("sync_hash_cache_test").toFile()
        syncCacheDir = File(tempDir, "sync").apply { mkdirs() }
        cache = SyncHashCache(syncCacheDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `getOrCompute returns valid SHA256 and caches it`() {
        val testFile = File(tempDir, "sample.txt").apply {
            writeBytes("Hello World".toByteArray())
        }
        val expectedHash = testFile.computeSHA256()

        val computedHash = cache.getOrCompute(testFile)

        assertEquals(expectedHash, computedHash)
        assertEquals(1, cache.size())
        assertEquals(expectedHash, cache.get(testFile))
    }

    @Test
    fun `getOrCompute returns cached hash on subsequent calls when file is untouched`() {
        val testFile = File(tempDir, "sample.txt").apply {
            writeBytes("Unchanged content".toByteArray())
        }
        val firstHash = cache.getOrCompute(testFile)
        val secondHash = cache.getOrCompute(testFile)

        assertEquals(firstHash, secondHash)
        assertEquals(1, cache.size())
    }

    @Test
    fun `getOrCompute recomputes hash when file size changes`() {
        val testFile = File(tempDir, "sample.txt").apply {
            writeBytes("Short".toByteArray())
        }
        val firstHash = cache.getOrCompute(testFile)

        // Modify file with longer content (size changes)
        testFile.writeBytes("Much longer content than before".toByteArray())
        testFile.setLastModified(System.currentTimeMillis() + 1000L)
        val newExpectedHash = testFile.computeSHA256()

        val secondHash = cache.getOrCompute(testFile)

        assertNotEquals(firstHash, secondHash)
        assertEquals(newExpectedHash, secondHash)
    }

    @Test
    fun `getOrCompute recomputes hash when mtime changes even if size is identical`() {
        // Equal size: 5 bytes
        val testFile = File(tempDir, "sample.txt").apply {
            writeBytes("ABCDE".toByteArray())
            setLastModified(1000000L)
        }
        val firstHash = cache.getOrCompute(testFile)

        // Same size (5 bytes), different content, updated mtime
        testFile.writeBytes("VWXYZ".toByteArray())
        testFile.setLastModified(2000000L)
        val newExpectedHash = testFile.computeSHA256()

        val secondHash = cache.getOrCompute(testFile)

        assertNotEquals(firstHash, secondHash)
        assertEquals(newExpectedHash, secondHash)
    }

    @Test
    fun `missing or empty file returns empty string and is not cached`() {
        val missingFile = File(tempDir, "non_existent.bin")
        assertEquals("", cache.getOrCompute(missingFile))
        assertNull(cache.get(missingFile))

        val emptyFile = File(tempDir, "empty.bin").apply { createNewFile() }
        assertEquals("", cache.getOrCompute(emptyFile))
        assertNull(cache.get(emptyFile))
        assertEquals(0, cache.size())
    }

    @Test
    fun `persistIfDirty saves cache to disk and new instance reloads it`() {
        val testFile1 = File(tempDir, "f1.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val testFile2 = File(tempDir, "f2.bin").apply { writeBytes(byteArrayOf(4, 5, 6, 7)) }

        val hash1 = cache.getOrCompute(testFile1)
        val hash2 = cache.getOrCompute(testFile2)

        val persistedFile = File(syncCacheDir, SyncHashCache.CACHE_FILE_NAME)
        assertFalse("Cache file should not exist prior to persistIfDirty", persistedFile.exists())

        cache.persistIfDirty()
        assertTrue("Cache file should exist after persistIfDirty", persistedFile.exists())
        assertTrue("Cache file should contain text", persistedFile.readText().isNotBlank())

        // Create fresh instance from same directory
        val reloadedCache = SyncHashCache(syncCacheDir)
        assertEquals(2, reloadedCache.size())
        assertEquals(hash1, reloadedCache.get(testFile1))
        assertEquals(hash2, reloadedCache.get(testFile2))
    }

    @Test
    fun `clear empties memory and removes disk file`() {
        val testFile = File(tempDir, "file.bin").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        cache.getOrCompute(testFile)
        cache.persistIfDirty()

        val persistedFile = File(syncCacheDir, SyncHashCache.CACHE_FILE_NAME)
        assertTrue(persistedFile.exists())

        cache.clear()
        assertEquals(0, cache.size())
        assertNull(cache.get(testFile))
        assertFalse(persistedFile.exists())
    }

    @Test
    fun `concurrent hash lookups are thread-safe`() = runBlocking {
        val files = (1..20).map { i ->
            File(tempDir, "concurrent_$i.bin").apply {
                writeBytes("Thread data $i".toByteArray())
            }
        }

        val deferreds = (1..50).map { idx ->
            async(Dispatchers.IO) {
                val file = files[idx % files.size]
                val hash = cache.getOrCompute(file)
                assertEquals(file.computeSHA256(), hash)
            }
        }
        deferreds.awaitAll()

        assertEquals(20, cache.size())
    }
}
