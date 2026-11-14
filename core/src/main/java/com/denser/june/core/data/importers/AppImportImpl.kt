package com.denser.june.core.data.importers

import android.content.Context
import android.net.Uri
import com.denser.june.core.domain.importers.AppImportRepo
import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.importers.ImportSummary
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppImportImpl(
    private val journalRepo: JournalRepository,
    private val context: Context
) : AppImportRepo {

    companion object {
        private const val TAG = "AppImport"
    }

    private val dailyYouImporter = DailyYouImporter(journalRepo, context)
    private val diariumImporter = DiariumImporter(journalRepo, context)
    private val dayOneImporter = DayOneImporter(journalRepo, context)
    private val storyPadImporter = StoryPadImporter(journalRepo, context)

    override suspend fun importFromUri(
        uri: Uri,
        onProgress: ((progress: Float, message: String) -> Unit)?
    ): Result<ImportSummary> = withContext(Dispatchers.IO) {
        val targetApp = FormatSniffer.sniff(context, uri)
            ?: return@withContext Result.failure(
                IllegalArgumentException("Could not detect the journal app format for this file.")
            )

        AppLogger.d(AppLogger.Category.BACKUP, TAG, "Resolved import source: $targetApp for URI: $uri")

        return@withContext when (targetApp) {
            ImportSource.DailyYou -> dailyYouImporter.import(uri, onProgress)
            ImportSource.Diarium -> diariumImporter.import(uri, onProgress)
            ImportSource.DayOne -> dayOneImporter.import(uri, onProgress)
            ImportSource.StoryPad -> storyPadImporter.import(uri, onProgress)
        }
    }
}
