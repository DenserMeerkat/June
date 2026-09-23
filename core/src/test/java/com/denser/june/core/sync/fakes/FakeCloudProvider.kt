package com.denser.june.core.sync.fakes

import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.core.domain.sync.RemoteFileMeta
import com.denser.june.core.domain.sync.SyncManifest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * In-memory FakeCloudProvider for unit testing.
 */
class FakeCloudProvider : CloudProvider {

    override val name: String = "FakeCloud"

    private val _journals = mutableMapOf<String, Pair<Journal, Long>>()
    private val _media = mutableMapOf<String, ByteArray>()
    var manifest: SyncManifest? = null

    var failNextConnect = false
    var failNextUpload = false
    var failNextDownload = false
    var failNextDeleteJournal = false
    var failNextDeleteMedia = false
    var failNextManifestRead = false
    var failNextManifestWrite = false

    var partialListingSize: Int = -1

    var uploadJournalCallCount = 0
    var downloadJournalCallCount = 0
    var uploadMediaCallCount = 0

    private val _isConnected = MutableStateFlow(true)
    override fun isConnected(): Flow<Boolean> = _isConnected

    override suspend fun connect(): Result<Unit> {
        if (failNextConnect) { failNextConnect = false; return Result.failure(Exception("Connect failed")) }
        _isConnected.value = true
        return Result.success(Unit)
    }

    override suspend fun disconnect() { _isConnected.value = false }

    fun putJournal(journal: Journal, modifiedAt: Long = System.currentTimeMillis()) {
        _journals[journal.id] = journal to modifiedAt
    }

    fun removeJournal(id: String) { _journals.remove(id) }
    fun getJournal(id: String): Journal? = _journals[id]?.first
    fun allJournalIds(): Set<String> = _journals.keys.toSet()

    override suspend fun uploadJournal(journal: Journal): Result<String> {
        if (failNextUpload) { failNextUpload = false; return Result.failure(Exception("Upload failed: 500")) }
        uploadJournalCallCount++
        val now = System.currentTimeMillis()
        _journals[journal.id] = journal to now
        return Result.success("${journal.id}.json")
    }

    override suspend fun downloadJournal(cloudId: String): Result<Journal> {
        if (failNextDownload) { failNextDownload = false; return Result.failure(Exception("Download failed: 500")) }
        downloadJournalCallCount++
        val id = cloudId.removeSuffix(".json")
        val entry = _journals[id] ?: return Result.failure(Exception("Not found: $cloudId"))
        return Result.success(entry.first)
    }

    override suspend fun deleteJournal(cloudId: String): Result<Unit> {
        if (failNextDeleteJournal) { failNextDeleteJournal = false; return Result.failure(Exception("Delete failed: 500")) }
        val id = cloudId.removeSuffix(".json")
        _journals.remove(id)
        return Result.success(Unit)
    }

    override suspend fun listJournals(): Result<List<RemoteFileMeta>> {
        val all = _journals.entries.map { (id, pair) ->
            RemoteFileMeta(name = "$id.json", lastModified = pair.second)
        }
        val result = if (partialListingSize >= 0) all.take(partialListingSize) else all
        return Result.success(result)
    }

    fun putMedia(journalId: String, filename: String, bytes: ByteArray = byteArrayOf(1, 2, 3)) {
        _media["$journalId/$filename"] = bytes
    }

    fun allMediaKeys(): Set<String> = _media.keys.toSet()

    var failNextUploadMedia = false
    var failNextDownloadMedia = false

    override suspend fun uploadMedia(journalId: String, file: File): Result<String> {
        if (failNextUploadMedia) { failNextUploadMedia = false; return Result.failure(Exception("Media upload failed")) }
        if (failNextUpload) { failNextUpload = false; return Result.failure(Exception("Media upload failed")) }
        uploadMediaCallCount++
        _media["$journalId/${file.name}"] = file.readBytes()
        return Result.success(file.name)
    }

    override suspend fun downloadMedia(journalId: String, cloudId: String, targetFile: File): Result<File> {
        if (failNextDownloadMedia) { failNextDownloadMedia = false; return Result.failure(Exception("Media download failed")) }
        if (failNextDownload) { failNextDownload = false; return Result.failure(Exception("Media download failed")) }
        val bytes = _media["$journalId/$cloudId"] ?: return Result.failure(Exception("Media not found"))
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(bytes)
        return Result.success(targetFile)
    }

    override suspend fun deleteMedia(journalId: String, filename: String): Result<Unit> {
        if (failNextDeleteMedia) { failNextDeleteMedia = false; return Result.failure(Exception("Media delete failed")) }
        _media.remove("$journalId/$filename")
        return Result.success(Unit)
    }

    override suspend fun listMedia(): Result<List<String>> {
        return Result.success(_media.keys.map { it.substringAfterLast("/") })
    }

    override suspend fun getManifest(): Result<SyncManifest?> {
        if (failNextManifestRead) { failNextManifestRead = false; return Result.failure(Exception("Manifest read failed")) }
        return Result.success(manifest)
    }

    override suspend fun updateManifest(manifest: SyncManifest): Result<Unit> {
        if (failNextManifestWrite) { failNextManifestWrite = false; return Result.failure(Exception("Manifest write failed")) }
        this.manifest = manifest
        return Result.success(Unit)
    }

    private val _songMedia = mutableMapOf<String, ByteArray>()
    var failNextUploadSongMedia = false
    var failNextDownloadSongMedia = false
    var failNextDeleteSongMedia = false

    fun putSongMedia(filename: String, bytes: ByteArray = byteArrayOf(1, 2, 3)) {
        _songMedia[filename] = bytes
    }

    fun allSongMediaKeys(): Set<String> = _songMedia.keys.toSet()

    override suspend fun uploadSongMedia(file: File): Result<String> {
        if (failNextUploadSongMedia) { failNextUploadSongMedia = false; return Result.failure(Exception("Song media upload failed")) }
        _songMedia[file.name] = file.readBytes()
        return Result.success(file.name)
    }

    override suspend fun downloadSongMedia(filename: String, targetFile: File): Result<File> {
        if (failNextDownloadSongMedia) { failNextDownloadSongMedia = false; return Result.failure(Exception("Song media download failed")) }
        val bytes = _songMedia[filename] ?: return Result.failure(Exception("Song media not found"))
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(bytes)
        return Result.success(targetFile)
    }

    override suspend fun deleteSongMedia(filename: String): Result<Unit> {
        if (failNextDeleteSongMedia) { failNextDeleteSongMedia = false; return Result.failure(Exception("Song media delete failed")) }
        _songMedia.remove(filename)
        return Result.success(Unit)
    }

    override suspend fun listSongMedia(): Result<List<String>> {
        return Result.success(_songMedia.keys.toList())
    }
}
