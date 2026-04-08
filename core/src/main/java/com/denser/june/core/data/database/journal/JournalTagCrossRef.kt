package com.denser.june.core.data.database.journal

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "journal_tag_cross_ref",
    primaryKeys = ["id", "tagId"],
    indices = [Index("tagId")]
)
data class JournalTagCrossRef(
    val id: String,
    val tagId: Long
)