package com.denser.june.core.domain.backup

sealed class RestoreException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object InvalidFile : RestoreException("Invalid or corrupted backup file")
    data object OldSchema : RestoreException("Unsupported database schema version")
}
