package com.denser.june.core.sync

import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.core.domain.sync.MediaSyncMeta
import com.denser.june.core.domain.sync.RemoteFileMeta
import com.denser.june.core.domain.sync.SyncManager
import com.denser.june.core.domain.sync.SyncManifest
import com.denser.june.core.sync.fakes.FakeSyncScheduler
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.SyncTestHarness
import com.denser.june.core.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Empirical Adversarial Challenger Test Suite:
 * 1. Size-Mismatch Fast-Fail:
 *    - Proves differing file lengths immediately mark files for upload/download WITHOUT invoking SHA-256 calculation.
 *    - Proves equal-size files with differing contents DO NOT bypass SHA-256 verification and are flagged for upload.
 *    - Proves equal-size files with identical contents match SHA-256 and are not re-uploaded.
 * 2. Concurrent Discovery:
 *    - Proves all 5 discovery queries in performAnalysis() execute in parallel.
 *    - Proves non-critical query failures (getManifest, listSongMedia, listAudios) degrade gracefully.
 *    - Proves critical query failures (listJournals, listMedia) cause performAnalysis() to fail.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncAdversarialChallengerTest {

    private lateinit var harness: SyncTestHarness

    @Before
    fun setUp() {
        harness = SyncTestHarness()
        harness.setUp()
    }

    @After
    fun tearDown() {
        harness.tearDown()
    }

    // =========================================================================
    // SECTION 1: Size-Mismatch Fast-Fail & Equal-Size Hash Enforcement
    // =========================================================================

    @Test
    fun `empirically prove size mismatch fast-fails without computing SHA256 during analysis`() = runTest {
        // Local file length = 100 bytes
        val localBytes = ByteArray(100) { 1 }
        val localMedia = harness.givenMediaFileOnDisk("mismatch_photo.jpg", localBytes)
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(localMedia.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "mismatch_photo.jpg", ByteArray(200) { 2 })

        // Remote manifest claims size = 200 bytes
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf(
                "mismatch_photo.jpg" to MediaSyncMeta(size = 200L, hash = "remote_claimed_hash")
            )
        )

        // Cache must be clean prior to analysis
        assertEquals("SyncHashCache must be empty initially", 0, harness.syncManager.syncHashCache.size())
        assertNull("Local file must not have cached hash prior to analysis", harness.syncManager.syncHashCache.get(localMedia))

        // Perform analysis
        val analysisResult = harness.performAnalysis()
        assertTrue("Analysis must succeed", analysisResult.isSuccess)
        val analysis = analysisResult.getOrThrow()

        // Size mismatch must mark the file for upload
        assertEquals("Mismatched size must be flagged for upload", 1, analysis.pendingMediaUploadsCount)

        // EMPIRICAL PROOF: SHA-256 computation was bypassed!
        // SyncHashCache was never queried/populated for this file because size-mismatch short-circuited first.
        assertNull("SHA256 must NOT be computed for size-mismatched file during analysis", harness.syncManager.syncHashCache.get(localMedia))
        assertEquals("SyncHashCache size must remain 0", 0, harness.syncManager.syncHashCache.size())
    }

    @Test
    fun `empirically prove equal-size files with differing contents DO NOT bypass hash check`() = runTest {
        // Local file length = 50 bytes with all 0xAA
        val localBytes = ByteArray(50) { 0xAA.toByte() }
        val localMedia = harness.givenMediaFileOnDisk("equal_size_different_content.jpg", localBytes)
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(localMedia.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)

        // Remote file has identical size (50 bytes) but different bytes (0xBB)
        val remoteBytes = ByteArray(50) { 0xBB.toByte() }
        harness.cloud.putMedia("j1", "equal_size_different_content.jpg", remoteBytes)

        val localHash = FileUtils.computeSHA256(localMedia)
        val remoteHash = "dummy_remote_hash_different_from_local"
        // Verify hashes differ despite identical file length
        assertFalse("Hashes must differ", remoteHash == localHash)

        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf(
                "equal_size_different_content.jpg" to MediaSyncMeta(size = 50L, hash = remoteHash)
            )
        )

        // Perform analysis
        val analysisResult = harness.performAnalysis()
        assertTrue(analysisResult.isSuccess)
        val analysis = analysisResult.getOrThrow()

        // Equal size with different hash MUST trigger pending upload
        assertEquals("Equal size with different hash MUST trigger upload", 1, analysis.pendingMediaUploadsCount)

        // EMPIRICAL PROOF: SHA-256 hash was computed and stored in cache because sizes matched
        assertNotNull("SHA256 MUST be computed when sizes match", harness.syncManager.syncHashCache.get(localMedia))
        assertEquals("Computed hash must match local file SHA256", localHash, harness.syncManager.syncHashCache.get(localMedia))

        // Now run sync: verify file is actually re-uploaded
        val syncResult = harness.sync()
        assertTrue(syncResult.isSuccess)
        assertEquals("Media with differing content must be re-uploaded", 1, harness.cloud.uploadMediaCallCount)
    }

    @Test
    fun `empirically prove equal-size files with identical contents match hash and skip upload`() = runTest {
        val bytes = ByteArray(64) { 0x77.toByte() }
        val localMedia = harness.givenMediaFileOnDisk("identical.jpg", bytes)
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(localMedia.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "identical.jpg", bytes)

        val expectedHash = FileUtils.computeSHA256(localMedia)
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf(
                "identical.jpg" to MediaSyncMeta(size = 64L, hash = expectedHash)
            )
        )

        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("Identical file must NOT trigger pending upload", 0, analysis.pendingMediaUploadsCount)

        val syncResult = harness.sync()
        assertTrue(syncResult.isSuccess)
        assertEquals("Identical file must NOT be re-uploaded", 0, harness.cloud.uploadMediaCallCount)
    }

    @Test
    fun `empirically prove size mismatch fast-fails for pendingMediaDownloads without computing local hash`() = runTest {
        // Local file exists with size 100 bytes
        val localFile = harness.givenMediaFileOnDisk("download_mismatch.jpg", ByteArray(100) { 1 })

        // Cloud has journal to download referencing this media, and remote size is 250 bytes
        harness.cloud.putMedia("j1", "download_mismatch.jpg", ByteArray(250) { 2 })
        harness.givenCloudOnlyJournal("j1", images = listOf("download_mismatch.jpg"), modifiedAt = T1)

        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf(
                "download_mismatch.jpg" to MediaSyncMeta(size = 250L, hash = "remote_hash_250")
            )
        )

        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("Size mismatch must flag file for download", 1, analysis.pendingMediaDownloadsCount)

        // Local hash was not computed
        assertNull("Local file hash must not be computed when remote size != local size", harness.syncManager.syncHashCache.get(localFile))
    }

    // =========================================================================
    // SECTION 2: Concurrent Discovery Execution & Fault Tolerance
    // =========================================================================

    private class ConcurrentTrackingCloudProvider(
        private val delegate: CloudProvider,
        private val simulatedDelayMs: Long = 60L
    ) : CloudProvider by delegate {
        val activeQueries = AtomicInteger(0)
        val maxConcurrentQueries = AtomicInteger(0)
        val completedQueries = AtomicInteger(0)

        private suspend fun <T> trackConcurrent(block: suspend () -> Result<T>): Result<T> {
            val current = activeQueries.incrementAndGet()
            maxConcurrentQueries.updateAndGet { maxOf(it, current) }
            try {
                if (simulatedDelayMs > 0) {
                    delay(simulatedDelayMs)
                }
                return block()
            } finally {
                activeQueries.decrementAndGet()
                completedQueries.incrementAndGet()
            }
        }

        override suspend fun getManifest(): Result<SyncManifest?> = trackConcurrent { delegate.getManifest() }
        override suspend fun listJournals(): Result<List<RemoteFileMeta>> = trackConcurrent { delegate.listJournals() }
        override suspend fun listMedia(): Result<List<String>> = trackConcurrent { delegate.listMedia() }
        override suspend fun listSongMedia(): Result<List<String>> = trackConcurrent { delegate.listSongMedia() }
        override suspend fun listAudios(): Result<List<String>> = trackConcurrent { delegate.listAudios() }
    }

    @Test
    fun `empirically prove all 5 discovery queries in performAnalysis run concurrently`() = runTest {
        val trackingProvider = ConcurrentTrackingCloudProvider(harness.cloud, simulatedDelayMs = 80L)

        val customSyncManager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("FakeCloud" to trackingProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir,
            audioDir = harness.audioDir,
            syncHashCache = harness.syncManager.syncHashCache
        )

        val result = customSyncManager.performAnalysis()
        assertTrue("performAnalysis should succeed", result.isSuccess)

        assertEquals("All 5 discovery queries must have executed", 5, trackingProvider.completedQueries.get())
        assertEquals(
            "Empirical proof of concurrency: max concurrent queries must be exactly 5",
            5,
            trackingProvider.maxConcurrentQueries.get()
        )
    }

    // --- Non-Critical Query Degradation ---

    private class FaultInjectingCloudProvider(
        private val delegate: CloudProvider,
        var failManifest: Boolean = false,
        var failSongMedia: Boolean = false,
        var failAudios: Boolean = false,
        var failJournals: Boolean = false,
        var failMedia: Boolean = false
    ) : CloudProvider by delegate {
        override suspend fun getManifest(): Result<SyncManifest?> {
            return if (failManifest) Result.failure(IOException("Simulated Manifest Failure"))
            else delegate.getManifest()
        }

        override suspend fun listSongMedia(): Result<List<String>> {
            return if (failSongMedia) Result.failure(IOException("Simulated SongMedia Failure"))
            else delegate.listSongMedia()
        }

        override suspend fun listAudios(): Result<List<String>> {
            return if (failAudios) Result.failure(IOException("Simulated Audio Failure"))
            else delegate.listAudios()
        }

        override suspend fun listJournals(): Result<List<RemoteFileMeta>> {
            return if (failJournals) Result.failure(IOException("Simulated Journals Failure"))
            else delegate.listJournals()
        }

        override suspend fun listMedia(): Result<List<String>> {
            return if (failMedia) Result.failure(IOException("Simulated Media Failure"))
            else delegate.listMedia()
        }
    }

    @Test
    fun `empirically prove non-critical manifest query failure degrades gracefully`() = runTest {
        val faultProvider = FaultInjectingCloudProvider(harness.cloud, failManifest = true)

        val manager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("FakeCloud" to faultProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir,
            audioDir = harness.audioDir,
            syncHashCache = harness.syncManager.syncHashCache
        )

        val result = manager.performAnalysis()
        assertTrue("Manifest failure must NOT abort performAnalysis()", result.isSuccess)
    }

    @Test
    fun `empirically prove non-critical songMedia query failure degrades gracefully`() = runTest {
        val faultProvider = FaultInjectingCloudProvider(harness.cloud, failSongMedia = true)

        val manager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("FakeCloud" to faultProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir,
            audioDir = harness.audioDir,
            syncHashCache = harness.syncManager.syncHashCache
        )

        val result = manager.performAnalysis()
        assertTrue("Song media listing failure must NOT abort performAnalysis()", result.isSuccess)
        assertEquals("remoteSongFiles should gracefully default to 0", 0, result.getOrThrow().remoteSongFiles)
    }

    @Test
    fun `empirically prove non-critical audios query failure degrades gracefully`() = runTest {
        val faultProvider = FaultInjectingCloudProvider(harness.cloud, failAudios = true)

        val manager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("FakeCloud" to faultProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir,
            audioDir = harness.audioDir,
            syncHashCache = harness.syncManager.syncHashCache
        )

        val result = manager.performAnalysis()
        assertTrue("Audios listing failure must NOT abort performAnalysis()", result.isSuccess)
        assertEquals("remoteAudioFiles should gracefully default to 0", 0, result.getOrThrow().remoteAudioFiles)
    }

    @Test
    fun `empirically prove simultaneous failure of all non-critical queries degrades gracefully`() = runTest {
        val faultProvider = FaultInjectingCloudProvider(
            harness.cloud,
            failManifest = true,
            failSongMedia = true,
            failAudios = true
        )

        val manager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("FakeCloud" to faultProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir,
            audioDir = harness.audioDir,
            syncHashCache = harness.syncManager.syncHashCache
        )

        val result = manager.performAnalysis()
        assertTrue("Simultaneous failure of all non-critical queries must still succeed", result.isSuccess)
    }

    // --- Critical Query Failures ---

    @Test
    fun `empirically prove critical journals query failure fails performAnalysis`() = runTest {
        val faultProvider = FaultInjectingCloudProvider(harness.cloud, failJournals = true)

        val manager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("FakeCloud" to faultProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir,
            audioDir = harness.audioDir,
            syncHashCache = harness.syncManager.syncHashCache
        )

        val result = manager.performAnalysis()
        assertTrue("Critical journals query failure MUST fail performAnalysis", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Simulated Journals Failure") == true)
    }

    @Test
    fun `empirically prove critical media query failure fails performAnalysis`() = runTest {
        val faultProvider = FaultInjectingCloudProvider(harness.cloud, failMedia = true)

        val manager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("FakeCloud" to faultProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir,
            audioDir = harness.audioDir,
            syncHashCache = harness.syncManager.syncHashCache
        )

        val result = manager.performAnalysis()
        assertTrue("Critical media query failure MUST fail performAnalysis", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Simulated Media Failure") == true)
    }
}
