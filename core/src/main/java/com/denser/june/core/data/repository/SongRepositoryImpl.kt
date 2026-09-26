package com.denser.june.core.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.denser.june.core.data.database.journal.JournalDao
import com.denser.june.core.data.database.song.SongLibraryDao
import com.denser.june.core.data.database.song.SongLibraryEntity
import com.denser.june.core.data.dto.SongLinkPageData
import com.denser.june.core.data.mappers.mapSongLinkPageDataToSongDetails
import com.denser.june.core.data.remote.DeezerFetcher
import com.denser.june.core.data.remote.ItunesFetcher
import com.denser.june.core.data.remote.SongLinkScraper
import com.denser.june.core.data.remote.SpotifyScraper
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.model.SongSourceType
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.MessageDigest
import com.denser.june.core.domain.model.SongFetchEvent
import androidx.core.net.toUri

class SongRepositoryImpl(
    private val songLinkScraper: SongLinkScraper,
    private val spotifyScraper: SpotifyScraper,
    private val deezerFetcher: DeezerFetcher,
    private val itunesFetcher: ItunesFetcher,
    private val songLibraryDao: SongLibraryDao,
    private val journalDao: JournalDao,
    private val okHttpClient: OkHttpClient,
    private val context: Context
) : SongRepository {

    private val songMediaDir = File(context.filesDir, "song_media")
    private val libraryDir = File(songMediaDir, "library").apply { if (!exists()) mkdirs() }
    private val artDir = File(songMediaDir, "art").apply { if (!exists()) mkdirs() }

    override fun fetchSongDetailsWithProgress(url: String): Flow<SongFetchEvent> = flow {
        try {
            emit(SongFetchEvent.Progress(1, 4, "Resolving song link…"))
            val pageData = songLinkScraper.fetchPageData(url)
                ?: return@flow emit(SongFetchEvent.Error(Exception("Could not resolve song details")))

            var details = mapSongLinkPageDataToSongDetails(pageData)

            emit(SongFetchEvent.Progress(2, 4, "Fetching preview…"))
            val candidates = resolvePreviewCandidates(pageData)
            details = details.copy(
                albumName = candidates.firstNotNullOfOrNull { it.albumName } ?: details.albumName,
                genre = candidates.firstNotNullOfOrNull { it.genre } ?: details.genre
            )

            emit(SongFetchEvent.Progress(3, 4, "Downloading artwork…"))
            val resolvedThumbnailUrl = details.thumbnailUrl ?: candidates.firstNotNullOfOrNull { it.artworkUrl }
            val localArt = resolvedThumbnailUrl?.let { cacheAlbumArt(it).getOrNull() }
            if (localArt != null) {
                details = details.copy(thumbnailUrl = resolvedThumbnailUrl, localThumbnailPath = localArt)
            }

            emit(SongFetchEvent.Progress(4, 4, "Downloading audio…"))
            val working = downloadFirstWorkingPreview(candidates)
            if (working != null) {
                val (candidate, audioPath) = working
                details = details.copy(
                    previewUrl = candidate.url,
                    previewUrlProvider = candidate.provider,
                    localPreviewPath = audioPath,
                    albumName = candidate.albumName ?: details.albumName,
                    genre = candidate.genre ?: details.genre
                )
                upsertToLibrary(details, audioPath, sourceUrl = url)
            } else {
                details = details.copy(
                    previewUrl = null,
                    previewUrlProvider = null,
                    localPreviewPath = null
                )
            }

            emit(SongFetchEvent.Success(details))
        } catch (e: Exception) {
            emit(SongFetchEvent.Error(e))
        }
    }

    private data class PreviewCandidate(
        val url: String,
        val provider: String,
        val albumName: String? = null,
        val genre: String? = null,
        val artworkUrl: String? = null
    )

    private suspend fun resolvePreviewCandidates(pageData: SongLinkPageData): List<PreviewCandidate> {
        val appleMusicId = pageData.appleMusicUrl?.toUri()?.let { uri ->
            uri.getQueryParameter("i")?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment?.takeIf { it.all(Char::isDigit) }
        }
        val deezerId = pageData.deezerUniqueId?.split("::", "|", "/")?.lastOrNull { it.all(Char::isDigit) }
            ?: pageData.deezerUrl?.split("?")?.firstOrNull()?.split("/")?.lastOrNull { it.all(Char::isDigit) }
        val spotifyId = pageData.spotifyUniqueId?.split("::", "|")?.lastOrNull()

        val (itunesDirect, deezerDirect, spotifyUrl) = coroutineScope {
            val itunes = async { appleMusicId?.let { itunesFetcher.fetchTrackData(it) } }
            val deezer = async { deezerId?.let { deezerFetcher.fetchTrackData(it) } }
            val spotify = async { spotifyId?.let { spotifyScraper.fetchPreviewUrl(it) } }
            Triple(itunes.await(), deezer.await(), spotify.await())
        }

        val (itunesSearch, deezerSearch) = if (itunesDirect?.previewUrl == null && deezerDirect?.previewUrl == null) {
            val query = "${pageData.title} ${pageData.artistName}".trim()
            if (query.isNotBlank()) {
                coroutineScope {
                    val itunes = async { itunesFetcher.searchTrack(query) }
                    val deezer = async { deezerFetcher.searchTrack(query) }
                    Pair(itunes.await(), deezer.await())
                }
            } else Pair(null, null)
        } else Pair(null, null)

        return listOfNotNull(
            itunesDirect?.previewUrl?.let { PreviewCandidate(it, "Apple Music", itunesDirect.albumName, itunesDirect.genre) },
            deezerDirect?.previewUrl?.let { PreviewCandidate(it, "Deezer", deezerDirect.albumName) },
            itunesSearch?.previewUrl?.let { PreviewCandidate(it, "Apple Music", itunesSearch.albumName, itunesSearch.genre, itunesSearch.artworkUrl) },
            deezerSearch?.previewUrl?.let { PreviewCandidate(it, "Deezer", deezerSearch.albumName, null, deezerSearch.coverUrl) },
            spotifyUrl?.takeIf { it.isNotBlank() }?.let { PreviewCandidate(it, "Spotify") }
        )
    }

    private suspend fun downloadFirstWorkingPreview(candidates: List<PreviewCandidate>): Pair<PreviewCandidate, String>? {
        for (candidate in candidates) {
            val result = downloadAndCachePreview(candidate.url, skipUpsert = true)
            val path = result.getOrNull()
            if (path != null && File(path).length() > 1024L) {
                return candidate to path
            }
        }
        return null
    }

    private suspend fun upsertToLibrary(
        song: SongDetails,
        localAudioPath: String,
        hash: String = File(localAudioPath).nameWithoutExtension,
        sourceUrl: String? = song.previewUrl
    ) {
        songLibraryDao.upsert(
            SongLibraryEntity(
                contentHash = hash,
                localPath = localAudioPath,
                localArtPath = song.localThumbnailPath,
                sourceUrl = sourceUrl,
                sourceType = song.sourceType.name,
                title = song.title,
                artistName = song.artistName,
                albumName = song.albumName,
                genre = song.genre,
                thumbnailUrl = song.thumbnailUrl,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun persistAndSyncSong(
        song: SongDetails,
        audioPath: String,
        artPath: String? = song.localThumbnailPath,
        sourceUrl: String? = song.previewUrl
    ): SongDetails {
        val hash = File(audioPath).nameWithoutExtension
        val resolvedArt = if ((artPath == null || !File(artPath).exists()) && song.thumbnailUrl != null) {
            cacheAlbumArt(song.thumbnailUrl, hash).getOrNull() ?: artPath
        } else artPath

        val updated = song.copy(localPreviewPath = audioPath, localThumbnailPath = resolvedArt)
        upsertToLibrary(updated, audioPath, hash, sourceUrl)
        updateMatchingJournalsArtwork(updated.title, updated.artistName, resolvedArt, audioPath)
        return updated
    }

    override suspend fun fetchSongDetails(url: String): Result<SongDetails> {
        var result: Result<SongDetails> = Result.failure(Exception("Failed to fetch song details"))
        fetchSongDetailsWithProgress(url).collect { event ->
            when (event) {
                is SongFetchEvent.Success -> result = Result.success(event.details)
                is SongFetchEvent.Error -> result = Result.failure(event.exception)
                is SongFetchEvent.Progress -> Unit
            }
        }
        return result
    }

    override suspend fun downloadAndCachePreview(
        previewUrl: String,
        songDetails: SongDetails?
    ): Result<String> = downloadAndCachePreview(previewUrl, songDetails, skipUpsert = false)

    private suspend fun downloadAndCachePreview(
        previewUrl: String,
        songDetails: SongDetails? = null,
        skipUpsert: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cachedByUrl = songLibraryDao.getBySourceUrl(previewUrl)
            if (cachedByUrl != null && File(cachedByUrl.localPath).exists() && File(cachedByUrl.localPath).length() > 0L) {
                return@withContext Result.success(cachedByUrl.localPath)
            }

            val request = Request.Builder().url(previewUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download audio preview: ${response.code}"))
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val tempFile = File(libraryDir, "preview_${System.currentTimeMillis()}.tmp")
            response.body?.byteStream()?.use { input ->
                tempFile.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        digest.update(buf, 0, read)
                        output.write(buf, 0, read)
                    }
                }
            } ?: return@withContext Result.failure(Exception("Empty preview response body"))

            val contentHash = digest.digest().joinToString("") { "%02x".format(it) }
            val targetFile = File(libraryDir, "$contentHash.mp3")
            val cachedByHash = songLibraryDao.getByHash(contentHash)
            if (cachedByHash != null && targetFile.exists() && targetFile.length() > 0L) {
                tempFile.delete()
                return@withContext Result.success(targetFile.absolutePath)
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            if (!skipUpsert) {
                upsertToLibrary(
                    song = songDetails ?: SongDetails(title = "Unknown Title", artistName = "Unknown Artist"),
                    localAudioPath = targetFile.absolutePath,
                    hash = contentHash,
                    sourceUrl = previewUrl
                )
            }

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.DATABASE, "SongRepositoryImpl", "Error caching preview audio", e)
            Result.failure(e)
        }
    }

    override suspend fun cacheAlbumArt(
        thumbnailUrl: String,
        songHash: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (songHash != null) {
                val existing = File(artDir, "$songHash.jpg")
                if (existing.exists() && existing.length() > 0L) {
                    return@withContext Result.success(existing.absolutePath)
                }
            }

            val request = Request.Builder().url(thumbnailUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download album art: ${response.code}"))
            }

            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty art response body"))

            val digest = MessageDigest.getInstance("SHA-256")
            val artHash = songHash ?: digest.digest(bytes).joinToString("") { "%02x".format(it) }
            val targetFile = File(artDir, "$artHash.jpg")
            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.writeBytes(bytes)
            }

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.DATABASE, "SongRepositoryImpl", "Error caching album art", e)
            Result.failure(e)
        }
    }

    override fun attachLocalAudioWithProgress(uri: Uri): Flow<SongFetchEvent> = flow {
        try {
            emit(SongFetchEvent.Progress(1, 2, "Importing audio file…"))
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                emit(SongFetchEvent.Error(Exception("Cannot open audio URI")))
                return@flow
            }

            val maxFileSizeBytes = 50L * 1024L * 1024L
            val maxDurationMs = 15 * 60 * 1000L

            val pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (_: Exception) {
                null
            }
            val initialSize = pfd?.statSize ?: 0L
            pfd?.close()
            if (initialSize > maxFileSizeBytes) {
                emit(SongFetchEvent.Error(Exception("Audio file exceeds maximum size limit (50 MB)")))
                return@flow
            }

            val tempFile = File(libraryDir, "import_${System.currentTimeMillis()}_${(0..999).random()}.tmp")
            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytesRead = 0L

            try {
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        totalBytesRead += read
                        if (totalBytesRead > maxFileSizeBytes) {
                            tempFile.delete()
                            emit(SongFetchEvent.Error(Exception("Audio file exceeds maximum size limit (50 MB)")))
                            return@flow
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            } finally {
                inputStream.close()
            }

            val contentHash = digest.digest().joinToString("") { "%02x".format(it) }
            val audioFile = File(libraryDir, "$contentHash.mp3")
            if (audioFile.exists() && audioFile.length() > 0L) {
                tempFile.delete()
            } else {
                if (!tempFile.renameTo(audioFile)) {
                    tempFile.copyTo(audioFile, overwrite = true)
                    tempFile.delete()
                }
            }

            emit(SongFetchEvent.Progress(2, 2, "Extracting metadata…"))
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var genre: String? = null
            var localArtPath: String? = null

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(audioFile.absolutePath)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (durationMs != null && durationMs > maxDurationMs) {
                    audioFile.delete()
                    emit(SongFetchEvent.Error(Exception("Audio duration exceeds maximum limit (15 minutes)")))
                    return@flow
                }

                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)

                val embeddedPicture = retriever.embeddedPicture
                if (embeddedPicture != null && embeddedPicture.isNotEmpty()) {
                    val artFile = File(artDir, "$contentHash.jpg")
                    artFile.writeBytes(embeddedPicture)
                    localArtPath = artFile.absolutePath
                }
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Category.DATABASE, "SongRepositoryImpl", "Failed to extract ID3 metadata: ${e.message}")
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }

            val resolvedTitle = title?.takeIf { it.isNotBlank() }
                ?: FileUtils.getDisplayName(context, uri)?.substringBeforeLast(".")
                ?: "Unknown Title"
            val resolvedArtist = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist"

            val songDetails = SongDetails(
                title = resolvedTitle,
                artistName = resolvedArtist,
                albumName = album,
                genre = genre,
                thumbnailUrl = null,
                localThumbnailPath = localArtPath,
                previewUrl = null,
                previewUrlProvider = null,
                localPreviewPath = audioFile.absolutePath,
                sourceType = SongSourceType.LOCAL_FILE
            )

            upsertToLibrary(songDetails, audioFile.absolutePath, contentHash)

            emit(SongFetchEvent.Success(songDetails))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.DATABASE, "SongRepositoryImpl", "Failed to attach local audio", e)
            emit(SongFetchEvent.Error(e))
        }
    }

    override suspend fun attachLocalAudio(uri: Uri): Result<SongDetails> {
        var result: Result<SongDetails> = Result.failure(Exception("Failed to attach audio"))
        attachLocalAudioWithProgress(uri).collect { event ->
            when (event) {
                is SongFetchEvent.Success -> result = Result.success(event.details)
                is SongFetchEvent.Error -> result = Result.failure(event.exception)
                is SongFetchEvent.Progress -> Unit
            }
        }
        return result
    }

    override suspend fun removeFromLibrary(song: SongDetails): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val path = song.localPreviewPath
            val hash = path?.let { File(it).nameWithoutExtension }
            if (hash != null) {
                songLibraryDao.deleteByHash(hash)
            }

            val allJournals = journalDao.getAllJournalsIncludeDeletedSync()
            val referencedAudioPaths = allJournals.mapNotNull { it.songDetails?.localPreviewPath }.toSet()
            val referencedArtPaths = allJournals.mapNotNull { it.songDetails?.localThumbnailPath }.toSet()

            if (path != null && path !in referencedAudioPaths) {
                val f = File(path)
                if (f.exists()) f.delete()
            }
            val art = song.localThumbnailPath
            if (art != null && art !in referencedArtPaths) {
                val af = File(art)
                if (af.exists()) af.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addToLibrary(song: SongDetails): Result<SongDetails> = withContext(Dispatchers.IO) {
        try {
            val resolvedLocal = FileUtils.resolveSongMedia(context, song.localPreviewPath, "library")
            if (resolvedLocal?.exists() == true && resolvedLocal.length() > 0L) {
                return@withContext Result.success(persistAndSyncSong(song, resolvedLocal.absolutePath))
            }

            val existingInLibrary = songLibraryDao.getByTitleAndArtist(song.title, song.artistName)
            if (existingInLibrary != null) {
                val resolvedLib = FileUtils.resolveSongMedia(context, existingInLibrary.localPath, "library")
                if (resolvedLib?.exists() == true && resolvedLib.length() > 0L) {
                    return@withContext Result.success(persistAndSyncSong(song, resolvedLib.absolutePath, existingInLibrary.localArtPath))
                }
            }

            if (song.previewUrl != null) {
                val downloaded = downloadAndCachePreview(song.previewUrl, song).getOrNull()
                if (downloaded != null) {
                    return@withContext Result.success(persistAndSyncSong(song, downloaded))
                }
            }

            val linkUrl = song.links.spotify
                ?: song.links.appleMusic
                ?: song.links.deezer
                ?: song.links.youtubeMusic
                ?: song.links.youtube
                ?: song.previewUrl

            if (linkUrl != null) {
                val fresh = fetchSongDetails(linkUrl).getOrNull()
                if (fresh != null) {
                    val resolvedAudio = FileUtils.resolveSongMedia(context, fresh.localPreviewPath, "library")
                    if (resolvedAudio?.exists() == true && resolvedAudio.length() > 0L) {
                        val merged = song.copy(
                            title = fresh.title.ifBlank { song.title },
                            artistName = fresh.artistName.ifBlank { song.artistName },
                            albumName = fresh.albumName ?: song.albumName,
                            genre = fresh.genre ?: song.genre,
                            thumbnailUrl = fresh.thumbnailUrl ?: song.thumbnailUrl,
                            previewUrl = fresh.previewUrl ?: song.previewUrl,
                            previewUrlProvider = fresh.previewUrlProvider ?: song.previewUrlProvider,
                            links = fresh.links
                        )
                        return@withContext Result.success(persistAndSyncSong(merged, resolvedAudio.absolutePath, fresh.localThumbnailPath, linkUrl))
                    }
                }
            }

            val query = "${song.title} ${song.artistName}".trim()
            if (query.isNotBlank()) {
                val (itunesSearch, deezerSearch) = coroutineScope {
                    val itunes = async { itunesFetcher.searchTrack(query) }
                    val deezer = async { deezerFetcher.searchTrack(query) }
                    Pair(itunes.await(), deezer.await())
                }

                if (itunesSearch?.previewUrl != null) {
                    val downloaded = downloadAndCachePreview(itunesSearch.previewUrl, song).getOrNull()
                    if (downloaded != null) {
                        val updated = song.copy(
                            albumName = song.albumName ?: itunesSearch.albumName,
                            genre = song.genre ?: itunesSearch.genre,
                            thumbnailUrl = song.thumbnailUrl ?: itunesSearch.artworkUrl,
                            previewUrl = itunesSearch.previewUrl,
                            previewUrlProvider = "Apple Music",
                            links = song.links.copy(appleMusic = itunesSearch.trackViewUrl ?: song.links.appleMusic)
                        )
                        return@withContext Result.success(persistAndSyncSong(updated, downloaded, sourceUrl = itunesSearch.previewUrl))
                    }
                }

                if (deezerSearch?.previewUrl != null) {
                    val downloaded = downloadAndCachePreview(deezerSearch.previewUrl, song).getOrNull()
                    if (downloaded != null) {
                        val updated = song.copy(
                            albumName = song.albumName ?: deezerSearch.albumName,
                            thumbnailUrl = song.thumbnailUrl ?: deezerSearch.coverUrl,
                            previewUrl = deezerSearch.previewUrl,
                            previewUrlProvider = "Deezer",
                            links = song.links.copy(deezer = deezerSearch.trackUrl ?: song.links.deezer)
                        )
                        return@withContext Result.success(persistAndSyncSong(updated, downloaded, sourceUrl = deezerSearch.previewUrl))
                    }
                }
            }

            Result.failure(Exception("No working preview found for \"${song.title}\""))
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.DATABASE, "SongRepositoryImpl", "Failed to add song to library", e)
            Result.failure(e)
        }
    }

    override suspend fun updateLibrarySongMeta(original: SongDetails, updated: SongDetails): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val path = original.localPreviewPath ?: updated.localPreviewPath
            val hash = path?.let { File(it).nameWithoutExtension }
            if (hash != null) {
                songLibraryDao.updateMetadata(
                    hash = hash,
                    title = updated.title,
                    artist = updated.artistName,
                    album = updated.albumName,
                    genre = updated.genre,
                    localArtPath = updated.localThumbnailPath
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupUnreferencedSongMedia(allJournals: List<Journal>): Unit = withContext(Dispatchers.IO) {
        try {
            val allLibraryEntries = songLibraryDao.getAll()
            val referencedAudioNames = (allJournals.mapNotNull { it.songDetails?.localPreviewPath } +
                    allLibraryEntries.map { it.localPath })
                .map { File(it).name }
                .toSet()

            libraryDir.listFiles()?.forEach { file ->
                if (file.name !in referencedAudioNames) {
                    file.delete()
                }
            }

            val referencedArtNames = (allJournals.mapNotNull { it.songDetails?.localThumbnailPath } +
                    allLibraryEntries.mapNotNull { it.localArtPath })
                .map { File(it).name }
                .toSet()

            artDir.listFiles()?.forEach { file ->
                if (file.name !in referencedArtNames) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.DATABASE, "SongRepositoryImpl", "Error cleaning up song media", e)
        }
    }

    override fun getLibrarySongs(): Flow<List<SongDetails>> {
        return songLibraryDao.observeAll().map { list ->
            list.map { entity ->
                SongDetails(
                    title = entity.title,
                    artistName = entity.artistName,
                    albumName = entity.albumName,
                    genre = entity.genre,
                    thumbnailUrl = entity.thumbnailUrl,
                    localThumbnailPath = entity.localArtPath,
                    previewUrl = entity.sourceUrl,
                    previewUrlProvider = null,
                    localPreviewPath = entity.localPath,
                    sourceType = try {
                        SongSourceType.valueOf(entity.sourceType)
                    } catch (_: Exception) {
                        SongSourceType.LINK
                    }
                )
            }
        }
    }

    private suspend fun updateMatchingJournalsArtwork(title: String, artist: String, artPath: String?, previewPath: String? = null) {
        try {
            val allJournals = journalDao.getAllJournalsIncludeDeletedSync()
            allJournals.forEach { entity ->
                val song = entity.songDetails
                if (song != null && song.title.equals(title, ignoreCase = true) && song.artistName.equals(artist, ignoreCase = true)) {
                    val needArtUpdate = artPath != null && song.localThumbnailPath != artPath
                    val needAudioUpdate = previewPath != null && song.localPreviewPath != previewPath
                    if (needArtUpdate || needAudioUpdate) {
                        val updatedSong = song.copy(
                            localThumbnailPath = artPath ?: song.localThumbnailPath,
                            localPreviewPath = previewPath ?: song.localPreviewPath
                        )
                        journalDao.updateJournal(entity.copy(songDetails = updatedSong))
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.DATABASE, "SongRepositoryImpl", "Failed to update matching journals artwork", e)
        }
    }
}