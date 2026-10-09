package com.denser.june.presentation.components.lightbox

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.media.exif.ExifUtils
import com.denser.june.core.media.exif.model.MediaExifInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LightboxMediaMetadataState(
    val exifInfo: MediaExifInfo?,
    val parentJournal: Journal?,
    val isMediaBackedUp: Boolean,
    val isPendingSync: Boolean,
    val syncProvider: String?
)

@Composable
fun rememberLightboxMediaMetadataState(
    context: Context,
    currentPath: String?,
    journalId: String?,
    fallbackDateTime: Long?,
    refreshTrigger: Int,
    newlyCompressedPaths: Set<String>,
    journalRepo: JournalRepository,
    syncPrefs: SyncPreferences
): LightboxMediaMetadataState {
    val isSyncEnabled by syncPrefs.getSyncEnabled().collectAsStateWithLifecycle(initialValue = false)
    val syncProvider by syncPrefs.getSelectedProvider().collectAsStateWithLifecycle(initialValue = null)

    var parentJournal by remember { mutableStateOf<Journal?>(null) }
    var allJournalsCache by remember { mutableStateOf<List<Journal>?>(null) }

    LaunchedEffect(journalId) {
        if (journalId == null) {
            allJournalsCache = withContext(Dispatchers.IO) { journalRepo.getAllJournals() }
        }
    }

    LaunchedEffect(currentPath, journalId, allJournalsCache) {
        if (currentPath == null) return@LaunchedEffect
        if (journalId != null) {
            parentJournal = journalRepo.getJournalById(journalId)
        } else {
            val fileName = File(currentPath).name
            parentJournal = allJournalsCache?.firstOrNull { j ->
                j.images.any { File(it).name == fileName }
            }
        }
    }

    var exifInfo by remember { mutableStateOf<MediaExifInfo?>(null) }

    val isPendingSync = remember(currentPath, newlyCompressedPaths, parentJournal, isSyncEnabled) {
        if (!isSyncEnabled || currentPath == null) {
            false
        } else if (newlyCompressedPaths.contains(currentPath)) {
            true
        } else {
            val journal = parentJournal
            if (journal != null && !journal.isDraft) {
                val syncedAt = journal.syncedAt
                val updatedAt = journal.updatedAt
                syncedAt == null || (updatedAt != null && updatedAt > syncedAt)
            } else {
                false
            }
        }
    }

    val isMediaBackedUp = remember(isSyncEnabled, parentJournal, currentPath, isPendingSync) {
        if (!isSyncEnabled || parentJournal == null || currentPath == null || isPendingSync) {
            false
        } else {
            val journal = parentJournal!!
            val fileName = File(currentPath).name
            val syncedAt = journal.syncedAt
            val updatedAt = journal.updatedAt
            !journal.isDraft &&
            syncedAt != null &&
            (updatedAt == null || syncedAt >= updatedAt) &&
            journal.images.any { File(it).name == fileName }
        }
    }

    LaunchedEffect(currentPath, refreshTrigger, parentJournal, isMediaBackedUp) {
        if (currentPath == null) return@LaunchedEffect
        val info = withContext(Dispatchers.IO) {
            ExifUtils.readMediaExif(
                context = context,
                path = currentPath,
                fallbackDateTime = fallbackDateTime ?: parentJournal?.dateTime,
                journalTitle = parentJournal?.title,
                journalMediaCount = parentJournal?.images?.size ?: 0,
                isBackedUp = isMediaBackedUp
            )
        }
        exifInfo = info
    }

    return remember(exifInfo, parentJournal, isMediaBackedUp, isPendingSync, syncProvider) {
        LightboxMediaMetadataState(
            exifInfo = exifInfo,
            parentJournal = parentJournal,
            isMediaBackedUp = isMediaBackedUp,
            isPendingSync = isPendingSync,
            syncProvider = syncProvider
        )
    }
}
