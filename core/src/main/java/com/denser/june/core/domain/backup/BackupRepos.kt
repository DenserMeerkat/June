package com.denser.june.core.domain.backup

import java.io.File

interface ExportRepo {
    suspend fun exportData(includeMedia: Boolean = true): Result<File>
    suspend fun exportAsMarkdown(includeMedia: Boolean = true): Result<File>
    suspend fun exportSingleJournalZip(journal: com.denser.june.core.domain.model.Journal): Result<File>
}

interface RestoreRepo {
    suspend fun restoreData(path: String): Result<Unit>
}