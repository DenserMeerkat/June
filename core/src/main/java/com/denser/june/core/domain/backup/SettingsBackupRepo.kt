package com.denser.june.core.domain.backup

import kotlinx.coroutines.flow.Flow

interface SettingsBackupRepo {
    suspend fun exportSettingsJson(includeMapKeys: Boolean = false): Result<String>
    suspend fun restoreSettingsJson(json: String): Result<Unit>
    fun hasExportableMapKeys(): Flow<Boolean>
}

