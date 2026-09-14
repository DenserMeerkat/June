package com.denser.june.presentation.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.markdown.MarkdownEngine
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.utils.FileUtils
import com.denser.june.presentation.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExternalIntentProcessor(
    private val context: Context,
    private val journalRepo: JournalRepository
) {
    companion object {
        private const val TAG = "ExternalIntentProcessor"
    }

    suspend fun processIntent(intent: Intent?): Route.Editor? = withContext(Dispatchers.IO) {
        if (intent == null) return@withContext null
        val action = intent.action ?: return@withContext null

        if (action != Intent.ACTION_VIEW &&
            action != Intent.ACTION_EDIT &&
            action != Intent.ACTION_SEND
        ) {
            return@withContext null
        }

        val uri: Uri? = intent.data
            ?: IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

        val textExtra: String? = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

        try {
            if (uri != null) {
                val displayName = FileUtils.getDisplayName(context, uri)
                val contentString = try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                } catch (e: Exception) {
                    AppLogger.e(AppLogger.Category.DATABASE, TAG, "Failed to read stream from URI: $uri", e)
                    null
                }

                if (!contentString.isNullOrBlank()) {
                    val journal = MarkdownEngine.fromMarkdown(contentString, displayName, isDraft = true)
                    val existing = journalRepo.getJournalById(journal.id)
                        ?: journalRepo.getAllJournals().firstOrNull {
                            it.isDraft && it.title == journal.title && it.content == journal.content
                        }
                    val targetId = if (existing != null) {
                        AppLogger.d(AppLogger.Category.DATABASE, TAG, "Reusing existing draft journal: ${existing.id}")
                        existing.id
                    } else {
                        journalRepo.insertJournal(journal)
                        AppLogger.d(AppLogger.Category.DATABASE, TAG, "Successfully created draft journal from intent URI: ${journal.id}")
                        journal.id
                    }
                    return@withContext Route.Editor(journalId = targetId)
                }
            }
            
            if (!textExtra.isNullOrBlank()) {
                val journal = MarkdownEngine.fromMarkdown(textExtra, "Shared Note", isDraft = true)
                val existing = journalRepo.getJournalById(journal.id)
                    ?: journalRepo.getAllJournals().firstOrNull {
                        it.isDraft && it.title == journal.title && it.content == journal.content
                    }
                val targetId = if (existing != null) {
                    AppLogger.d(AppLogger.Category.DATABASE, TAG, "Reusing existing draft journal from shared text: ${existing.id}")
                    existing.id
                } else {
                    journalRepo.insertJournal(journal)
                    AppLogger.d(AppLogger.Category.DATABASE, TAG, "Successfully created draft journal from shared text: ${journal.id}")
                    journal.id
                }
                return@withContext Route.Editor(journalId = targetId)
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.DATABASE, TAG, "Failed to process external intent", e)
        }

        null
    }
}
