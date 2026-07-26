package com.denser.june.core.sync.fixtures

import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.sync.SyncManifest

/**
 * Fixed timestamps for readable, deterministic tests.
 *
 *  T0 = epoch baseline (all journals "born" here)
 *  T1 = first sync point
 *  T2 = device A edits (after T1)
 *  T3 = device B edits (after T1, concurrent with T2)
 *  T4 = second sync point
 */
object SyncFixtures {

    const val T0 = 1_000_000L
    const val T1 = T0 + 10_000L   // synced at
    const val T2 = T1 + 5_000L    // device A edit (after syncedAt)
    const val T3 = T1 + 3_000L    // device B edit (after syncedAt, < T2)
    const val T4 = T2 + 10_000L   // next sync time

    const val THRESHOLD = 2_000L   // SyncManager.SYNC_THRESHOLD_MS

    // --- Journal builders ---

    /** A journal that has never been synced. */
    fun newJournal(
        id: String = "journal-new",
        title: String = "New Journal",
        content: String = "Hello world",
        updatedAt: Long = T0,
        images: List<String> = emptyList()
    ) = Journal(
        id = id,
        title = title,
        content = content,
        createdAt = T0,
        updatedAt = updatedAt,
        dateTime = T0,
        syncedAt = null,
        cloudId = null,
        images = images
    )

    /** A journal that was synced at T1 and not modified since. */
    fun syncedJournal(
        id: String = "journal-synced",
        title: String = "Synced Journal",
        syncedAt: Long = T1,
        updatedAt: Long = T1
    ) = Journal(
        id = id,
        title = title,
        content = "Synced content",
        createdAt = T0,
        updatedAt = updatedAt,
        dateTime = T0,
        syncedAt = syncedAt,
        cloudId = "$id.json"
    )

    /** A journal modified on Device A after the last sync. */
    fun deviceAModified(
        id: String = "journal-conflict",
        title: String = "Modified by A"
    ) = syncedJournal(id = id).copy(
        title = title,
        content = "Device A content",
        updatedAt = T2   // after syncedAt=T1
    )

    /** Same journal ID, modified on Device B after the last sync. */
    fun deviceBModified(
        id: String = "journal-conflict",
        title: String = "Modified by B"
    ) = syncedJournal(id = id).copy(
        title = title,
        content = "Device B content",
        updatedAt = T3   // after syncedAt=T1 but < T2
    )

    /** A soft-deleted journal (in bin). */
    fun deletedJournal(
        id: String = "journal-deleted",
        deletedAt: Long = T2
    ) = newJournal(id = id).copy(
        deletedAt = deletedAt,
        updatedAt = deletedAt,
        syncedAt = T1,
        cloudId = "$id.json"
    )

    // --- Manifest builders ---

    fun manifest(
        totalJournals: Int = 0,
        deletedIds: List<String> = emptyList(),
        schemaVersion: Int = 3,
        deviceId: String = "test-device-A",
        journalMetadata: Map<String, com.denser.june.core.domain.sync.JournalSyncMeta> = emptyMap(),
        mediaMetadata: Map<String, com.denser.june.core.domain.sync.MediaSyncMeta> = emptyMap()
    ) = SyncManifest(
        lastSyncTime = T1,
        lastSyncDeviceId = deviceId,
        databaseVersion = 4,
        schemaVersion = schemaVersion,
        totalJournals = totalJournals,
        totalMedia = 0,
        deletedIds = deletedIds,
        journalMetadata = journalMetadata,
        mediaMetadata = mediaMetadata
    )
}
