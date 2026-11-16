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

data class ImportBatchResult(
    val importedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val errors: List<String>
)

object ImporterHelper {

    fun generateDeterministicId(source: ImportSource, externalId: String): String {
        return UUID.nameUUIDFromBytes("${source.name}_$externalId".toByteArray(Charsets.UTF_8)).toString()
    }

    class ImportSession(
        private val journalRepo: JournalRepository,
        private val chunkSize: Int = 500,
        private val onProgress: ((Float, String) -> Unit)? = null
    ) {
        private var existingMetadataLoaded = false
        private val existingById = mutableMapOf<String, com.denser.june.core.data.database.journal.JournalMetadataEntity>()
        private val existingByDateTimeTitle = mutableMapOf<Pair<Long, String>, com.denser.june.core.data.database.journal.JournalMetadataEntity>()

        var importedCount = 0
            private set
        var updatedCount = 0
            private set
        var skippedCount = 0
            private set
        val errors = mutableListOf<String>()

        private val toInsertBuffer = ArrayList<Journal>(chunkSize)
        private val toUpdateBuffer = ArrayList<Journal>(chunkSize)

        suspend fun init() {
            if (!existingMetadataLoaded) {
                val metadataList = journalRepo.getAllJournalMetadata()
                metadataList.forEach { meta ->
                    existingById[meta.id] = meta
                    existingByDateTimeTitle[Pair(meta.dateTime, meta.title)] = meta
                }
                existingMetadataLoaded = true
            }
        }

        suspend fun processChunk(incomingList: List<Journal>) {
            init()
            for (incoming in incomingList) {
                processSingle(incoming)
            }
            flushBuffersIfNeeded()
        }

        private suspend fun processSingle(incoming: Journal) {
            try {
                val existing = existingById[incoming.id]
                    ?: existingByDateTimeTitle[Pair(incoming.dateTime, incoming.title)]

                if (existing == null) {
                    toInsertBuffer.add(incoming)
                    val meta = com.denser.june.core.data.database.journal.JournalMetadataEntity(
                        id = incoming.id,
                        dateTime = incoming.dateTime,
                        title = incoming.title,
                        createdAt = incoming.createdAt,
                        updatedAt = incoming.updatedAt,
                        deletedAt = incoming.deletedAt,
                        syncedAt = incoming.syncedAt,
                        cloudId = incoming.cloudId
                    )
                    existingById[incoming.id] = meta
                    existingByDateTimeTitle[Pair(incoming.dateTime, incoming.title)] = meta
                    importedCount++
                } else if (existing.deletedAt != null) {
                    val restoredJournal = incoming.copy(
                        id = existing.id,
                        cloudId = existing.cloudId,
                        deletedAt = null,
                        syncedAt = null,
                        updatedAt = System.currentTimeMillis()
                    )
                    toUpdateBuffer.add(restoredJournal)
                    val meta = existing.copy(
                        deletedAt = null,
                        syncedAt = null,
                        updatedAt = restoredJournal.updatedAt
                    )
                    existingById[existing.id] = meta
                    existingByDateTimeTitle[Pair(existing.dateTime, existing.title)] = meta
                    updatedCount++
                } else {
                    val incomingUpdated = incoming.updatedAt ?: incoming.createdAt
                    val existingUpdated = existing.updatedAt ?: existing.createdAt

                    if (incomingUpdated >= existingUpdated) {
                        val fullExisting = journalRepo.getJournalById(existing.id)
                        if (fullExisting != null && fullExisting.isContentEqualTo(incoming)) {
                            skippedCount++
                        } else {
                            val finalJournal = incoming.copy(
                                id = existing.id,
                                cloudId = existing.cloudId,
                                syncedAt = null,
                                deletedAt = null
                            )
                            toUpdateBuffer.add(finalJournal)
                            val meta = existing.copy(
                                deletedAt = null,
                                syncedAt = null,
                                updatedAt = finalJournal.updatedAt
                            )
                            existingById[existing.id] = meta
                            existingByDateTimeTitle[Pair(existing.dateTime, existing.title)] = meta
                            updatedCount++
                        }
                    } else {
                        val fullExisting = journalRepo.getJournalById(existing.id)
                        if (fullExisting != null) {
                            val mergedImages = (fullExisting.images + incoming.images).distinct()
                            val mergedAudios = (fullExisting.audios + incoming.audios).distinctBy { it.path }
                            val mergedTags = (fullExisting.tags + incoming.tags).distinct()
                            val mergedLocation = fullExisting.location ?: incoming.location
                            val mergedEmoji = fullExisting.emoji ?: incoming.emoji
                            val mergedTheme = fullExisting.theme ?: incoming.theme
                            val mergedFont = fullExisting.font ?: incoming.font

                            val enriched = fullExisting.copy(
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
                            if (enriched.isContentEqualTo(fullExisting)) {
                                skippedCount++
                            } else {
                                toUpdateBuffer.add(enriched)
                                val meta = existing.copy(
                                    deletedAt = null,
                                    syncedAt = null,
                                    updatedAt = enriched.updatedAt
                                )
                                existingById[existing.id] = meta
                                existingByDateTimeTitle[Pair(existing.dateTime, existing.title)] = meta
                                updatedCount++
                            }
                        } else {
                            toInsertBuffer.add(incoming)
                            importedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                errors.add("Failed to process entry ${incoming.title}: ${e.message}")
            }
        }

        private suspend fun flushBuffersIfNeeded() {
            if (toInsertBuffer.size >= chunkSize) {
                journalRepo.insertJournalsBatch(toInsertBuffer.toList())
                toInsertBuffer.clear()
            }
            if (toUpdateBuffer.size >= chunkSize) {
                journalRepo.updateJournalsBatch(toUpdateBuffer.toList())
                toUpdateBuffer.clear()
            }
        }

        suspend fun finish(): ImportBatchResult {
            if (toInsertBuffer.isNotEmpty()) {
                journalRepo.insertJournalsBatch(toInsertBuffer.toList())
                toInsertBuffer.clear()
            }
            if (toUpdateBuffer.isNotEmpty()) {
                journalRepo.updateJournalsBatch(toUpdateBuffer.toList())
                toUpdateBuffer.clear()
            }
            onProgress?.invoke(1f, "Import completed")
            return ImportBatchResult(
                importedCount = importedCount,
                updatedCount = updatedCount,
                skippedCount = skippedCount,
                errors = errors
            )
        }
    }

    suspend fun processAndSaveBatch(
        journalRepo: JournalRepository,
        incomingJournals: List<Journal>,
        chunkSize: Int = 500,
        onProgress: ((Float, String) -> Unit)? = null
    ): ImportBatchResult {
        if (incomingJournals.isEmpty()) {
            return ImportBatchResult(0, 0, 0, emptyList())
        }

        return journalRepo.runInTransaction {
            val session = ImportSession(journalRepo, chunkSize, onProgress)
            val total = incomingJournals.size
            var processed = 0
            incomingJournals.chunked(chunkSize).forEach { chunk ->
                session.processChunk(chunk)
                processed += chunk.size
                onProgress?.invoke(
                    (processed.toFloat() / total).coerceIn(0f, 1f),
                    "Processing $processed of $total"
                )
            }
            session.finish()
        }
    }

    suspend fun saveOrUpdateJournal(
        journalRepo: JournalRepository,
        incoming: Journal,
        existingCache: Map<Pair<Long, String>, Journal>? = null
    ): ImportEntryResult {
        val existing = existingCache?.get(Pair(incoming.dateTime, incoming.title))
            ?: journalRepo.getJournalById(incoming.id)

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
