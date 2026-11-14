package com.denser.june.core.data.importers

import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import java.util.UUID

enum class ImportEntryResult {
    Inserted,
    Updated,
    Skipped
}

object ImporterHelper {

    fun generateDeterministicId(source: ImportSource, externalId: String): String {
        return UUID.nameUUIDFromBytes("${source.name}_$externalId".toByteArray(Charsets.UTF_8)).toString()
    }

    suspend fun saveOrUpdateJournal(
        journalRepo: JournalRepository,
        incoming: Journal,
        existingCache: Map<Pair<Long, String>, Journal>? = null
    ): ImportEntryResult {
        val existing = journalRepo.getJournalById(incoming.id)
            ?: existingCache?.get(Pair(incoming.dateTime, incoming.title))

        if (existing == null) {
            journalRepo.insertJournal(incoming)
            return ImportEntryResult.Inserted
        }

        if (existing.isDeleted) {
            val restoredJournal = incoming.copy(
                id = existing.id,
                cloudId = existing.cloudId,
                deletedAt = null,
                syncedAt = null,
                updatedAt = System.currentTimeMillis()
            )
            journalRepo.updateJournal(restoredJournal)
            return ImportEntryResult.Updated
        }

        if (existing.isContentEqualTo(incoming)) {
            return ImportEntryResult.Skipped
        }

        val incomingUpdated = incoming.updatedAt ?: incoming.createdAt
        val existingUpdated = existing.updatedAt ?: existing.createdAt

        return if (incomingUpdated >= existingUpdated) {
            val updatedJournal = incoming.copy(
                id = existing.id,
                cloudId = existing.cloudId,
                syncedAt = null,
                deletedAt = null
            )
            journalRepo.updateJournal(updatedJournal)
            ImportEntryResult.Updated
        } else {
            val mergedImages = (existing.images + incoming.images).distinct()
            val mergedAudios = (existing.audios + incoming.audios).distinctBy { it.path }
            val mergedTags = (existing.tags + incoming.tags).distinct()
            val mergedLocation = existing.location ?: incoming.location
            val mergedEmoji = existing.emoji ?: incoming.emoji
            val mergedTheme = existing.theme ?: incoming.theme
            val mergedFont = existing.font ?: incoming.font

            val enrichedJournal = existing.copy(
                images = mergedImages,
                audios = mergedAudios,
                tags = mergedTags,
                location = mergedLocation,
                emoji = mergedEmoji,
                theme = mergedTheme,
                font = mergedFont,
                updatedAt = System.currentTimeMillis(),
                syncedAt = null,
                deletedAt = null
            )

            if (enrichedJournal.isContentEqualTo(existing)) {
                ImportEntryResult.Skipped
            } else {
                journalRepo.updateJournal(enrichedJournal)
                ImportEntryResult.Updated
            }
        }
    }
}
