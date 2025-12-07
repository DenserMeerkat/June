package com.example.june.core.domain.backup

sealed interface ExportState {
    data object Exporting: ExportState
    data class ExportReady(val data: String): ExportState
    data object Error: ExportState
}

sealed interface RestoreState {
    data object Idle : RestoreState
    data object Restoring : RestoreState
    data object Restored : RestoreState
    data class Failure(val exception: RestoreFailedException) : RestoreState
}

sealed interface RestoreFailedException {
    data object InvalidFile : RestoreFailedException
    data object OldSchema : RestoreFailedException
}

sealed class RestoreResult {
    data object Success : RestoreResult()
    data class Failure(val exceptionType: RestoreFailedException) : RestoreResult()
}