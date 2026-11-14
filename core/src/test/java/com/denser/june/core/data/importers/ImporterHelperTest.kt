package com.denser.june.core.data.importers

import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.sync.fakes.FakeJournalRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImporterHelperTest {

    private lateinit var repo: FakeJournalRepository

    @Before
    fun setUp() {
        repo = FakeJournalRepository()
    }

    @Test
    fun testReimportUnchangedJournalIsSkipped() = runTest {
        val deterministicId = ImporterHelper.generateDeterministicId(ImportSource.DayOne, "ENTRY-1")
        val timestamp = 1700000000000L

        val existing = Journal(
            id = deterministicId,
            title = "A Day in Paris",
            content = "Walking along the Seine.",
            createdAt = timestamp,
            updatedAt = timestamp,
            dateTime = timestamp,
            images = listOf("import_DayOne_photo1.jpg"),
            tags = listOf("#travel"),
            isDraft = false
        )
        repo.insertJournal(existing)

        // Incoming is identical
        val incoming = existing.copy()

        val result = ImporterHelper.saveOrUpdateJournal(repo, incoming)
        assertEquals("Identical journal re-import must be Skipped", ImportEntryResult.Skipped, result)
    }

    @Test
    fun testReimportWithNewMediaUpdatesJournal() = runTest {
        val deterministicId = ImporterHelper.generateDeterministicId(ImportSource.DayOne, "ENTRY-2")
        val timestamp = 1700000000000L

        // Previously imported without images due to old importer bug
        val existing = Journal(
            id = deterministicId,
            title = "Trip to Tokyo",
            content = "Shibuya crossing.",
            createdAt = timestamp,
            updatedAt = timestamp,
            dateTime = timestamp,
            images = emptyList(),
            isDraft = false
        )
        repo.insertJournal(existing)

        // Now imported with image extracted from backup (same backup timestamps)
        val incoming = existing.copy(
            images = listOf("import_DayOne_shibuya.jpg"),
            content = "Shibuya crossing.\n\n![](import_DayOne_shibuya.jpg)"
        )

        val result = ImporterHelper.saveOrUpdateJournal(repo, incoming)
        assertEquals("Should update journal when media is added", ImportEntryResult.Updated, result)

        val updatedInRepo = repo.getJournalById(deterministicId)
        assertNotNull(updatedInRepo)
        assertEquals(1, updatedInRepo?.images?.size)
        assertEquals("import_DayOne_shibuya.jpg", updatedInRepo?.images?.first())
        assertTrue(updatedInRepo?.content?.contains("import_DayOne_shibuya.jpg") == true)
        assertNull("syncedAt must be reset to null to trigger cloud upload", updatedInRepo?.syncedAt)
    }

    @Test
    fun testReimportRestoresSoftDeletedJournalFromBin() = runTest {
        val deterministicId = ImporterHelper.generateDeterministicId(ImportSource.DailyYou, "ENTRY-3")
        val timestamp = 1700000000000L

        // Journal was moved to Bin by the user in June
        val softDeleted = Journal(
            id = deterministicId,
            title = "Morning Run",
            content = "Ran 5km in the park.",
            createdAt = timestamp,
            updatedAt = System.currentTimeMillis(),
            dateTime = timestamp,
            deletedAt = System.currentTimeMillis(), // in the Bin
            isDraft = false
        )
        repo.insertJournal(softDeleted)
        assertTrue("Precondition: journal is in Bin", repo.getJournalById(deterministicId)?.isDeleted == true)

        // Re-importing backup file containing this entry
        val incoming = Journal(
            id = deterministicId,
            title = "Morning Run (Fixed)",
            content = "Ran 5km in the park with audio memo.",
            createdAt = timestamp,
            updatedAt = timestamp,
            dateTime = timestamp,
            isDraft = false
        )

        val result = ImporterHelper.saveOrUpdateJournal(repo, incoming)
        assertEquals("Re-importing a soft-deleted journal must un-delete and update it", ImportEntryResult.Updated, result)

        val restoredInRepo = repo.getJournalById(deterministicId)
        assertNotNull(restoredInRepo)
        assertFalse("Journal must be active, not deleted", restoredInRepo?.isDeleted == true)
        assertNull("deletedAt must be null", restoredInRepo?.deletedAt)
        assertNull("syncedAt must be null so sync uploads the restored entry", restoredInRepo?.syncedAt)
        assertEquals("Morning Run (Fixed)", restoredInRepo?.title)
    }

    @Test
    fun testReimportPreservesLocalJuneEditsWhileAddingMedia() = runTest {
        val deterministicId = ImporterHelper.generateDeterministicId(ImportSource.StoryPad, "ENTRY-4")
        val backupTime = 1700000000000L
        val userEditTime = backupTime + 100_000L

        // User edited the journal in June after initial import
        val locallyEdited = Journal(
            id = deterministicId,
            title = "My Custom June Title",
            content = "I added my personal thoughts inside June.",
            createdAt = backupTime,
            updatedAt = userEditTime, // newer than backup
            dateTime = backupTime,
            images = emptyList(),
            tags = listOf("#JuneEdit"),
            isDraft = false
        )
        repo.insertJournal(locallyEdited)

        // Incoming backup from StoryPad with original text, but newly parsed images
        val incoming = Journal(
            id = deterministicId,
            title = "Original StoryPad Title",
            content = "Original text from StoryPad.",
            createdAt = backupTime,
            updatedAt = backupTime,
            dateTime = backupTime,
            images = listOf("import_StoryPad_sunset.jpg"),
            tags = listOf("#Nature"),
            isDraft = false
        )

        val result = ImporterHelper.saveOrUpdateJournal(repo, incoming)
        assertEquals(ImportEntryResult.Updated, result)

        val finalJournal = repo.getJournalById(deterministicId)
        assertNotNull(finalJournal)
        // Verify June's manual text edits were PRESERVED
        assertEquals("My Custom June Title", finalJournal?.title)
        assertEquals("I added my personal thoughts inside June.", finalJournal?.content)
        // Verify missing media was ENRICHED
        assertEquals(listOf("import_StoryPad_sunset.jpg"), finalJournal?.images)
        // Verify tags were merged
        assertTrue(finalJournal?.tags?.contains("#JuneEdit") == true)
        assertTrue(finalJournal?.tags?.contains("#Nature") == true)
    }

    @Test
    fun testFallbackMatchingByDateTimeAndTitle() = runTest {
        val originalId = "RANDOM-UUID-999"
        val timestamp = 1700000000000L

        val existing = Journal(
            id = originalId,
            title = "Coffee with Alice",
            content = "Great conversation at Central Perk.",
            createdAt = timestamp,
            updatedAt = timestamp,
            dateTime = timestamp,
            images = emptyList(),
            isDraft = false
        )
        repo.insertJournal(existing)

        val existingCache = mapOf(Pair(timestamp, "Coffee with Alice") to existing)

        // Incoming has a different deterministic ID or no ID, but matches (dateTime, title)
        val incoming = Journal(
            id = "DIFFERENT-ID-123",
            title = "Coffee with Alice",
            content = "Great conversation at Central Perk with photos.",
            createdAt = timestamp,
            updatedAt = timestamp,
            dateTime = timestamp,
            images = listOf("import_Diarium_cafe.jpg"),
            isDraft = false
        )

        val result = ImporterHelper.saveOrUpdateJournal(repo, incoming, existingCache)
        assertEquals(ImportEntryResult.Updated, result)

        // Must update the existing journal row under originalId, not insert a duplicate
        val updatedOriginal = repo.getJournalById(originalId)
        assertNotNull("Must update existing journal with original ID", updatedOriginal)
        assertEquals(1, updatedOriginal?.images?.size)
        assertNull("Must not insert under the different ID", repo.getJournalById("DIFFERENT-ID-123"))
    }
}

