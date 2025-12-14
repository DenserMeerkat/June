package com.example.june.core.data.mappers

import com.example.june.core.data.database.journal.JournalEntity
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.domain.backup.JournalSchema


fun JournalEntity.toJournal(): Journal {
    return Journal(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime,
        isBookmarked = isBookmarked,
        isArchived = isArchived,
        isDraft = isDraft
    )
}

fun Journal.toEntity(): JournalEntity {
    return JournalEntity(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime,
        isBookmarked = isBookmarked,
        isArchived = isArchived,
        isDraft = isDraft
    )
}

fun Journal.toJournalSchema(): JournalSchema {
    return JournalSchema(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime,
        isBookmarked = isBookmarked,
        isArchived = isArchived,
        isDraft = isDraft
    )
}

fun JournalSchema.toJournal(): Journal {
    return Journal(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime,
        isBookmarked = isBookmarked,
        isArchived = isArchived,
        isDraft = isDraft
    )
}
