package com.example.june.core.data.mappers

import com.example.june.core.data.database.NoteEntity
import com.example.june.core.domain.data_classes.Note
import com.example.june.core.domain.backup.NoteSchema


fun NoteEntity.toNote(): Note {
    return Note(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime
    )
}

fun Note.toNoteSchema(): NoteSchema {
    return NoteSchema(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime
    )
}

fun NoteSchema.toNote(): Note {
    return Note(
        id = id,
        title = title,
        content = content,
        coverImageUri = coverImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
        dateTime = dateTime
    )
}
