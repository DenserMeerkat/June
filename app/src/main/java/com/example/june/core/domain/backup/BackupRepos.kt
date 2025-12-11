package com.example.june.core.domain.backup

interface ExportRepo {
    suspend fun exportToJson(): String?
}

interface RestoreRepo {
    suspend fun restoreJournals(path: String): RestoreResult
}