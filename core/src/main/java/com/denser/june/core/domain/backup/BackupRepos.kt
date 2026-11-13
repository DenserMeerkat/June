package com.denser.june.core.domain.backup

import java.io.File

interface ExportRepo {
    suspend fun getPreBackupCounts(): PreBackupCounts

    suspend fun exportData(
        includeMedia: Boolean = true,
        includeAudios: Boolean = true,
        includeSongFiles: Boolean = true,
        includeSettings: Boolean = true,
        includeMapKeys: Boolean = false,
        onProgress: ((BackupOperationStatus.InProgress) -> Unit)? = null
    ): Result<File>

    suspend fun exportAsMarkdown(
        includeMedia: Boolean = true,
        includeAudios: Boolean = true,
        includeSongFiles: Boolean = true,
        onProgress: ((BackupOperationStatus.InProgress) -> Unit)? = null
    ): Result<File>

    suspend fun exportSingleJournalZip(journal: com.denser.june.core.domain.model.Journal): Result<File>
}

interface RestoreRepo {
    suspend fun restoreData(
        path: String,
        onProgress: ((BackupOperationStatus.InProgress) -> Unit)? = null
    ): Result<BackupSummary>
}
