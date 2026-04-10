package com.denser.june.core.data.mappers

import com.denser.june.core.data.database.journal.JournalEntity
import com.denser.june.core.domain.model.Journal

fun JournalEntity.asDomain(): Journal {
    return Journal(
        id = id,
        title = title,
        content = content,
        emoji = emoji,
        images = images,
        location = location,
        songDetails = songDetails,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime,
        isBookmarked = isBookmarked,
        isArchived = isArchived,
        isDraft = isDraft,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        cloudId = cloudId
    )
}

fun Journal.asEntity(): JournalEntity {
    return JournalEntity(
        id = id,
        title = title,
        content = content,
        emoji = emoji,
        images = images,
        location = location,
        songDetails = songDetails,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime,
        isBookmarked = isBookmarked,
        isArchived = isArchived,
        isDraft = isDraft,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        cloudId = cloudId
    )
}

fun List<JournalEntity>.asDomain(): List<Journal> = map { it.asDomain() }
fun List<Journal>.asEntity(): List<JournalEntity> = map { it.asEntity() }