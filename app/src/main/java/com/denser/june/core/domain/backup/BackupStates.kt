package com.denser.june.core.domain.backup

import java.io.File

sealed interface ExportState {
    data object Idle: ExportState
    data object Exporting: ExportState
    data object Error: ExportState
    data class ExportReady(val file: File): ExportState
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