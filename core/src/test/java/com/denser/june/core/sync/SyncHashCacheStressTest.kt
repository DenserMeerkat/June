package com.denser.june.core.sync

import com.denser.june.core.domain.sync.SyncHashCache
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.utils.computeSHA256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.random.Random

class SyncHashCacheStressTest {

    private lateinit var tempDir: File
    private lateinit var syncCacheDir: File
    private lateinit var cache: SyncHashCache

    @Before
    fun setUp() {
        tempDir = createTempDirectory("sync_stress_test").toFile()
        syncCacheDir = File(tempDir, "sync").apply { mkdirs() }
        cache = SyncHashCache(syncCacheDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // =========================================================================
    // 1. Corruption Resilience
    // =========================================================================

    @Test
    fun `resilience - 0-byte sha256_cache json does not crash and is healed on persist`() {
        val cacheFile = File(syncCacheDir, SyncHashCache.CACHE_FILE_NAME).apply {
            createNewFile()
        }
        assertEquals(0L, cacheFile.length())

        // Load new instance pointing to 0-byte file
        val newCache = SyncHashCache(syncCacheDir)
        assertEquals(0, newCache.size())

        // Compute and persist
        val file = File(tempDir, "f.txt").apply { writeBytes("Data".toByteArray()) }
        val hash = newCache.getOrCompute(file)
        newCache.persistIfDirty()

        assertTrue(cacheFile.length() > 0L)
        val healedCache = SyncHashCache(syncCacheDir)
        assertEquals(1, healedCache.size())
        assertEquals(hash, healedCache.get(file))
    }

    @Test
    fun `resilience - whitespace-only sha256_cache json does not crash`() {
        val cacheFile = File(syncCacheDir, SyncHashCache.CACHE_FILE_NAME).apply {
            writeText("   \n\t  \r\n   ")
        }

        val newCache = SyncHashCache(syncCacheDir)
        assertEquals(0, newCache.size())
    }

    @Test
    fun `resilience - truncated json does not crash and starts with empty cache`() {
        val cacheFile = File(syncCacheDir, SyncHashCache.CACHE_FILE_NAME).apply {
            writeText("""{"/data/test.jpg": {"lastModified": 1234567, "size": 100""")
        }

        val newCache = SyncHashCache(syncCacheDir)
        assertEquals(0, newCache.size())

        // Writing valid entry heals disk file
        val file = File(tempDir, "sample.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val hash = newCache.getOrCompute(file)
        newCache.persistIfDirty()

        val reloaded = SyncHashCache(syncCacheDir)
        assertEquals(1, reloaded.size())
        assertEquals(hash, reloaded.get(file))
    }

    @Test
    fun `resilience - random binary garbage on disk does not crash`() {
        val cacheFile = File(syncCacheDir, SyncHashCache.CACHE_FILE_NAME).apply {
            writeBytes(byteArrayOf(0x1F, 0x8B.toByte(), 0x08, 0x00, 0xFF.toByte(), 0xFE.toByte()))
        }

        val newCache = SyncHashCache(syncCacheDir)
        assertEquals(0, newCache.size())
    }

    @Test
    fun `resilience - schema mismatch (json array instead of object) does not crash`() {
        val cacheFile = File(syncCacheDir, SyncHashCache.CACHE_FILE_NAME).apply {
            writeText("""[{"lastModified": 123, "size": 10, "hash": "abc"}]""")
        }

        val newCache = SyncHashCache(syncCacheDir)
        assertEquals(0, newCache.size())
    }

    @Test
    fun `resilience - leftover tmp file does not prevent successful persistence`() {
        val tmpFile = File(syncCacheDir, "${SyncHashCache.CACHE_FILE_NAME}.tmp").apply {
            writeText("corrupt orphan tmp data")
        }
        assertTrue(tmpFile.exists())

        val testFile = File(tempDir, "photo.jpg").apply { writeBytes("PhotoContent".toByteArray()) }
        val hash = cache.getOrCompute(testFile)
        cache.persistIfDirty()

        val reloaded = SyncHashCache(syncCacheDir)
        assertEquals(1, reloaded.size())
        assertEquals(hash, reloaded.get(testFile))
    }

    // =========================================================================
    // 2. Adversarial Edge Cases: File Lifecycle (Deletion, Truncation, Recreation)
    // =========================================================================

    @Test
    fun `edge case - non-existent file returns empty string and null get`() {
        val nonExistent = File(tempDir, "does_not_exist.bin")
        assertEquals("", cache.getOrCompute(nonExistent))
        assertNull(cache.get(nonExistent))
        assertEquals(0, cache.size())
    }

    @Test
    fun `edge case - 0-byte file returns empty string and null get`() {
        val empty = File(tempDir, "empty.bin").apply { createNewFile() }
        assertEquals("", cache.getOrCompute(empty))
        assertNull(cache.get(empty))
        assertEquals(0, cache.size())
    }

    @Test
    fun `edge case - cached file deleted from disk returns empty string and null get`() {
        val file = File(tempDir, "file_to_delete.bin").apply { writeBytes(byteArrayOf(10, 20, 30)) }
        val initialHash = cache.getOrCompute(file)
        assertFalse(initialHash.isBlank())

        // File is deleted
        file.delete()
        assertFalse(file.exists())

        // Cache must recognize file is gone
        assertEquals("", cache.getOrCompute(file))
        assertNull(cache.get(file))
    }

    @Test
    fun `edge case - cached file truncated to 0 bytes returns empty string and null get`() {
        val file = File(tempDir, "file_to_truncate.bin").apply { writeBytes(byteArrayOf(10, 20, 30)) }
        val initialHash = cache.getOrCompute(file)
        assertFalse(initialHash.isBlank())

        // Truncate to 0 bytes
        file.writeBytes(ByteArray(0))
        assertEquals(0L, file.length())

        assertEquals("", cache.getOrCompute(file))
        assertNull(cache.get(file))
    }

    @Test
    fun `edge case - cached file deleted and recreated with same name but new content is recomputed`() {
        val file = File(tempDir, "recreated.bin").apply {
            writeBytes("Original Content".toByteArray())
            setLastModified(1000000L)
        }
        val initialHash = cache.getOrCompute(file)

        // Delete file
        file.delete()

        // Recreate with different content and new timestamp
        file.writeBytes("Different Longer Content".toByteArray())
        file.setLastModified(2000000L)

        val newHash = cache.getOrCompute(file)
        assertNotEquals(initialHash, newHash)
        assertEquals(file.computeSHA256(), newHash)
    }

    // =========================================================================
    // 3. In-Place Timestamp Tampering / Compression Simulation
    // =========================================================================

    @Test
    fun `in-place modification - size changed with restored timestamp recomputes hash`() {
        // Simulates CompressUtils.applyCompressedFile:
        // Original file compressed in place, and origTimestamp restored on target file
        val file = File(tempDir, "photo.jpg").apply {
            writeBytes(ByteArray(1000) { 1.toByte() })
            setLastModified(1500000L)
        }
        val originalHash = cache.getOrCompute(file)

        // Compress: size reduces from 1000 to 500, but lastModified is explicitly preserved
        val savedTimestamp = file.lastModified()
        file.writeBytes(ByteArray(500) { 2.toByte() })
        file.setLastModified(savedTimestamp)

        // Cache must NOT return stale hash because file.length() != cached.size
        val compressedHash = cache.getOrCompute(file)
        assertNotEquals("Hash must not match stale entry when size changed despite same timestamp", originalHash, compressedHash)
        assertEquals(file.computeSHA256(), compressedHash)
    }

    @Test
    fun `in-place modification - undo restore uncompressed original restores original hash`() {
        // Simulates CompressUtils.restoreUncompressedOriginalFile:
        val originalBytes = ByteArray(1000) { 1.toByte() }
        val file = File(tempDir, "photo.jpg").apply {
            writeBytes(originalBytes)
            setLastModified(1500000L)
        }
        val originalHash = cache.getOrCompute(file)

        // Compress
        val origTimestamp = file.lastModified()
        file.writeBytes(ByteArray(500) { 2.toByte() })
        file.setLastModified(origTimestamp)
        val compressedHash = cache.getOrCompute(file)
        assertNotEquals(originalHash, compressedHash)

        // Now restore original uncompressed file with original size and timestamp
        file.writeBytes(originalBytes)
        file.setLastModified(origTimestamp)

        // Cache should match (mtime, size) and return originalHash
        val restoredHash = cache.getOrCompute(file)
        assertEquals(originalHash, restoredHash)
    }

    // =========================================================================
    // 4. High-Concurrency Stress Hammering
    // =========================================================================

    @Test
    fun `concurrency - high volume multi-threaded hammering with concurrent read write persist and clear`() = runBlocking {
        val fileCount = 30
        val files = (0 until fileCount).map { i ->
            File(tempDir, "stress_$i.bin").apply {
                writeBytes("Payload number $i with random salt ${Random.nextLong()}".toByteArray())
            }
        }

        val iterationsPerWorker = 50
        val workerCount = 16
        val errorCount = AtomicInteger(0)

        val workers = (0 until workerCount).map { workerId ->
            async(Dispatchers.IO) {
                try {
                    for (step in 0 until iterationsPerWorker) {
                        val file = files[(workerId + step) % files.size]

                        when (step % 5) {
                            0, 1 -> {
                                val hash = cache.getOrCompute(file)
                                val expected = file.computeSHA256()
                                if (hash != expected) {
                                    errorCount.incrementAndGet()
                                }
                            }
                            2 -> {
                                cache.get(file)
                            }
                            3 -> {
                                cache.persistIfDirty()
                            }
                            4 -> {
                                // Put explicit value
                                cache.put(file, file.computeSHA256())
                            }
                        }
                    }
                } catch (t: Throwable) {
                    errorCount.incrementAndGet()
                }
            }
        }

        workers.awaitAll()
        assertEquals("No errors allowed during concurrent hammering", 0, errorCount.get())

        // Final persistence test
        cache.persistIfDirty()
        val reloaded = SyncHashCache(syncCacheDir)
        assertTrue("Reloaded cache must have cached entries", reloaded.size() > 0)
    }

    // =========================================================================
    // 5. File Comparison Invariants: Equal-Size Diff vs Unequal-Size Fast-Fail
    // =========================================================================

    @Test
    fun `file comparison - equal size differing bytes must NOT fast fail as equal`() {
        val file1 = File(tempDir, "audio_take1.m4a").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04))
        }
        val file2 = File(tempDir, "audio_take2.m4a").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x09)) // 1 byte diff, same size 5
        }

        assertEquals(file1.length(), file2.length())

        val hash1 = cache.getOrCompute(file1)
        val hash2 = cache.getOrCompute(file2)
        assertNotEquals("Equal length differing content must produce distinct hashes", hash1, hash2)

        // Simulate Variant B comparison:
        // remoteMeta.size = 5, remoteMeta.hash = hash2
        val remoteSize = file2.length()
        val remoteHash = hash2

        val isSizeDifferent = (remoteSize > 0L && file1.length() != remoteSize)
        assertFalse("Size is identical, fast-fail must NOT trigger", isSizeDifferent)

        // Hash comparison must trigger mismatch:
        val isHashDifferent = (remoteHash.isNotBlank() && hash1 != remoteHash)
        assertTrue("Hash comparison must correctly detect difference despite equal file size", isHashDifferent)
    }

    @Test
    fun `file comparison - unequal size fast-fails without needing hash calculation`() {
        val localFile = File(tempDir, "img.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        }
        val remoteSize = 10L

        // Variant B check:
        val fastFailTriggered = (remoteSize > 0L && localFile.length() != remoteSize)
        assertTrue("Unequal size must fast fail immediately", fastFailTriggered)
    }
}
