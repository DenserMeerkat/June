package com.denser.june.core.domain.backup

sealed class BackupOperationStatus {
    data object Idle : BackupOperationStatus()
    data class InProgress(
        val phase: String = "",
        val current: Int = 0,
        val total: Int = 0,
        val percentage: Float = 0f,
        val currentItemName: String = ""
    ) : BackupOperationStatus()
    data class Success(val message: String, val summary: BackupSummary? = null) : BackupOperationStatus()
    data class Error(val message: String, val cause: Throwable? = null) : BackupOperationStatus()
}

data class BackupSummary(
    val totalJournals: Int = 0,
    val totalMedia: Int = 0,
    val totalAudios: Int = 0,
    val totalSongs: Int = 0,
    val hasSettings: Boolean = false
)

data class PreBackupCounts(
    val totalJournals: Int = 0,
    val totalMedia: Int = 0,
    val totalAudios: Int = 0,
    val totalSongs: Int = 0,
    val hasSettings: Boolean = true,
    val hasMapKeys: Boolean = false
)

