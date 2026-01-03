package com.example.june.core.data.mappers

import com.example.june.core.data.database.journal.JournalEntity
import com.example.june.core.domain.data_classes.Journal


fun JournalEntity.toJournal(): Journal {
    return Journal(
        id = id,
        title = title,
        content = content,
        emoji = emoji,
        images = images,
        location = location,
        songDetails = songDetails,
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
        emoji = emoji,
        images = images,
        location = location,
        songDetails = songDetails,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime,
        isBookmarked = isBookmarked,
        isArchived = isArchived,
        isDraft = isDraft
    )
}