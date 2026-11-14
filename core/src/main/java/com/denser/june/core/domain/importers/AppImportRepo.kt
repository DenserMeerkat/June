package com.denser.june.core.domain.importers

import android.net.Uri

interface AppImportRepo {
    suspend fun importFromUri(
        uri: Uri,
        onProgress: ((progress: Float, message: String) -> Unit)? = null
    ): Result<ImportSummary>
}
