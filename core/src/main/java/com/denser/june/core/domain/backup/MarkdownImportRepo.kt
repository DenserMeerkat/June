package com.denser.june.core.domain.backup

import android.net.Uri

interface MarkdownImportRepo {
    suspend fun importMarkdownFiles(uris: List<Uri>): Result<Int>
    suspend fun importMarkdownZip(uri: Uri): Result<Int>
}
