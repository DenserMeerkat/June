package com.denser.june.core.domain.backup

import java.io.File

interface ExportRepo {
    suspend fun exportData(includeMedia: Boolean = true): File?
}

interface RestoreRepo {
    suspend fun restoreData(path: String): RestoreResult
}