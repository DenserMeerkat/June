package com.denser.june.core.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.denser.june.core.data.database.journal.JournalDao
import com.denser.june.core.data.database.song.SongLibraryDao
import com.denser.june.core.data.database.song.SongLibraryEntity
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.MessageDigest
import com.denser.june.core.domain.model.SongFetchEvent

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
            if (pageData == null) {
                emit(SongFetchEvent.Error(Exception("Could not resolve song details")))
                return@flow
            }

            var details = mapSongLinkPageDataToSongDetails(pageData)

            emit(SongFetchEvent.Progress(2, 4, "Fetching preview…"))
            var previewUrl: String? = null
            var previewProvider: String? = null
            var albumName: String? = details.albumName
            var genre: String? = details.genre

            val spotifyId = pageData.spotifyUniqueId?.split("::")?.lastOrNull()
                ?: pageData.spotifyUniqueId?.split("|")?.lastOrNull()

            if (spotifyId != null) {
                previewUrl = spotifyScraper.fetchPreviewUrl(spotifyId)
                if (previewUrl != null) previewProvider = "Spotify"
            }

            if (pageData.appleMusicUrl != null) {
                val appleMusicId = pageData.appleMusicUrl
                    .substringAfterLast("/")
                    .substringBefore("?")
                if (appleMusicId.isNotBlank()) {
                    val itunesData = itunesFetcher.fetchTrackData(appleMusicId)
                    if (itunesData != null) {
                        if (previewUrl == null && itunesData.previewUrl != null) {
                            previewUrl = itunesData.previewUrl
                            previewProvider = "Apple Music"
                        }
                        if (albumName.isNullOrBlank() && !itunesData.albumName.isNullOrBlank()) {
                            albumName = itunesData.albumName
                        }
                        if (genre.isNullOrBlank() && !itunesData.genre.isNullOrBlank()) {
                            genre = itunesData.genre
                        }
                    }
                }
            }

            val deezerId = pageData.deezerUniqueId?.split("|")?.lastOrNull()
            if (deezerId != null) {
                val deezerData = deezerFetcher.fetchTrackData(deezerId)
                if (deezerData != null) {
                    if (previewUrl == null && deezerData.previewUrl != null) {
                        previewUrl = deezerData.previewUrl
                        previewProvider = "Deezer"
                    }
                    if (albumName.isNullOrBlank() && !deezerData.albumName.isNullOrBlank()) {
                        albumName = deezerData.albumName
                    }
                }
            }

            details = details.copy(
                previewUrl = previewUrl ?: details.previewUrl,
                previewUrlProvider = previewProvider ?: details.previewUrlProvider,
                albumName = albumName,
                genre = genre
            )

            emit(SongFetchEvent.Progress(3, 4, "Downloading artwork…"))
            val artResult = details.thumbnailUrl?.let { cacheAlbumArt(it) }
            val localArt = artResult?.getOrNull()
            if (localArt != null) {
                details = details.copy(localThumbnailPath = localArt)
            }

            emit(SongFetchEvent.Progress(4, 4, "Downloading audio…"))
            if (details.previewUrl != null) {
                val previewResult = downloadAndCachePreview(details.previewUrl!!, details)
                val localPreview = previewResult.getOrNull()
                if (localPreview != null) {
                    details = details.copy(localPreviewPath = localPreview)
                    val hash = File(localPreview).nameWithoutExtension
                    songLibraryDao.upsert(
                        SongLibraryEntity(
                            contentHash = hash,
                            localPath = localPreview,
                            localArtPath = details.localThumbnailPath,
                            sourceUrl = url,
                            sourceType = details.sourceType.name,
                            title = details.title,
                            artistName = details.artistName,
                            albumName = details.albumName,
                            genre = details.genre,
                            thumbnailUrl = details.thumbnailUrl,
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            emit(SongFetchEvent.Success(details))
        } catch (e: Exception) {
            emit(SongFetchEvent.Error(e))
        }
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

            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty preview response body"))

            val digest = MessageDigest.getInstance("SHA-256")
            val contentHash = digest.digest(bytes).joinToString("") { "%02x".format(it) }

            val targetFile = File(libraryDir, "$contentHash.mp3")
            val cachedByHash = songLibraryDao.getByHash(contentHash)
            if (cachedByHash != null && targetFile.exists() && targetFile.length() > 0L) {
                return@withContext Result.success(targetFile.absolutePath)
            }

            targetFile.writeBytes(bytes)

            val entry = SongLibraryEntity(
                contentHash = contentHash,
                localPath = targetFile.absolutePath,
                localArtPath = songDetails?.localThumbnailPath,
                sourceUrl = previewUrl,
                sourceType = songDetails?.sourceType?.name ?: "LINK",
                title = songDetails?.title ?: "Unknown Title",
                artistName = songDetails?.artistName ?: "Unknown Artist",
                albumName = songDetails?.albumName,
                genre = songDetails?.genre,
                thumbnailUrl = songDetails?.thumbnailUrl,
                addedAt = System.currentTimeMillis()
            )
            songLibraryDao.upsert(entry)

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

            songLibraryDao.upsert(
                SongLibraryEntity(
                    contentHash = contentHash,
                    localPath = audioFile.absolutePath,
                    localArtPath = localArtPath,
                    sourceUrl = null,
                    sourceType = "LOCAL_FILE",
                    title = resolvedTitle,
                    artistName = resolvedArtist,
                    albumName = album,
                    genre = genre,
                    thumbnailUrl = null,
                    addedAt = System.currentTimeMillis()
                )
            )

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
            val localPreview = song.localPreviewPath
            if (localPreview != null && File(localPreview).exists() && File(localPreview).length() > 0L) {
                val hash = File(localPreview).nameWithoutExtension
                var artPath = song.localThumbnailPath
                if ((artPath == null || !File(artPath).exists()) && song.thumbnailUrl != null) {
                    val artResult = cacheAlbumArt(song.thumbnailUrl, hash)
                    artPath = artResult.getOrNull()
                }
                val finalArtPath = artPath ?: song.localThumbnailPath
                songLibraryDao.upsert(
                    SongLibraryEntity(
                        contentHash = hash,
                        localPath = localPreview,
                        localArtPath = finalArtPath,
                        sourceUrl = song.previewUrl ?: song.links.spotify ?: song.links.deezer,
                        sourceType = song.sourceType.name,
                        title = song.title,
                        artistName = song.artistName,
                        albumName = song.albumName,
                        genre = song.genre,
                        thumbnailUrl = song.thumbnailUrl,
                        addedAt = System.currentTimeMillis()
                    )
                )
                if (finalArtPath != song.localThumbnailPath) {
                    updateMatchingJournalsArtwork(song.title, song.artistName, finalArtPath)
                }
                val updated = song.copy(localThumbnailPath = finalArtPath)
                return@withContext Result.success(updated)
            }

            if (song.previewUrl != null) {
                val previewResult = downloadAndCachePreview(song.previewUrl, song)
                if (previewResult.isSuccess) {
                    val downloadedPath = previewResult.getOrThrow()
                    val hash = File(downloadedPath).nameWithoutExtension
                    var artPath = song.localThumbnailPath
                    if ((artPath == null || !File(artPath).exists()) && song.thumbnailUrl != null) {
                        val artResult = cacheAlbumArt(song.thumbnailUrl, hash)
                        artPath = artResult.getOrNull()
                    }
                    val finalArtPath = artPath ?: song.localThumbnailPath
                    songLibraryDao.updateMetadata(
                        hash = hash,
                        title = song.title,
                        artist = song.artistName,
                        album = song.albumName,
                        genre = song.genre,
                        localArtPath = finalArtPath
                    )
                    if (finalArtPath != null) {
                        updateMatchingJournalsArtwork(song.title, song.artistName, finalArtPath, downloadedPath)
                    }
                    val updated = song.copy(
                        localPreviewPath = downloadedPath,
                        localThumbnailPath = finalArtPath
                    )
                    return@withContext Result.success(updated)
                }
            }

            val linkUrl = song.links.spotify
                ?: song.links.appleMusic
                ?: song.links.deezer
                ?: song.links.youtubeMusic
                ?: song.links.youtube
                ?: song.previewUrl

            if (linkUrl != null) {
                val freshResult = fetchSongDetails(linkUrl)
                if (freshResult.isSuccess) {
                    val fresh = freshResult.getOrThrow()
                    val finalArtPath = fresh.localThumbnailPath ?: song.localThumbnailPath
                    val finalAudioPath = fresh.localPreviewPath ?: song.localPreviewPath
                    if (finalArtPath != null || finalAudioPath != null) {
                        updateMatchingJournalsArtwork(song.title, song.artistName, finalArtPath, finalAudioPath)
                    }
                    val merged = song.copy(
                        title = fresh.title.ifBlank { song.title },
                        artistName = fresh.artistName.ifBlank { song.artistName },
                        albumName = fresh.albumName ?: song.albumName,
                        genre = fresh.genre ?: song.genre,
                        thumbnailUrl = fresh.thumbnailUrl ?: song.thumbnailUrl,
                        localThumbnailPath = finalArtPath,
                        previewUrl = fresh.previewUrl ?: song.previewUrl,
                        previewUrlProvider = fresh.previewUrlProvider ?: song.previewUrlProvider,
                        localPreviewPath = finalAudioPath,
                        links = fresh.links
                    )
                    return@withContext Result.success(merged)
                }
            }

            val query = "${song.title} ${song.artistName}".trim()
            if (query.isNotBlank()) {
                val itunesSearch = itunesFetcher.searchTrack(query)
                if (itunesSearch != null && itunesSearch.previewUrl != null) {
                    val previewResult = downloadAndCachePreview(itunesSearch.previewUrl, song)
                    if (previewResult.isSuccess) {
                        val downloadedPath = previewResult.getOrThrow()
                        val hash = File(downloadedPath).nameWithoutExtension
                        var artPath = song.localThumbnailPath
                        val artUrl = itunesSearch.artworkUrl ?: song.thumbnailUrl
                        if ((artPath == null || !File(artPath).exists()) && artUrl != null) {
                            val artResult = cacheAlbumArt(artUrl, hash)
                            artPath = artResult.getOrNull()
                        }
                        val finalArtPath = artPath ?: song.localThumbnailPath
                        songLibraryDao.updateMetadata(
                            hash = hash,
                            title = song.title,
                            artist = song.artistName,
                            album = song.albumName ?: itunesSearch.albumName,
                            genre = song.genre ?: itunesSearch.genre,
                            localArtPath = finalArtPath
                        )
                        updateMatchingJournalsArtwork(song.title, song.artistName, finalArtPath, downloadedPath)
                        val updated = song.copy(
                            albumName = song.albumName ?: itunesSearch.albumName,
                            genre = song.genre ?: itunesSearch.genre,
                            thumbnailUrl = song.thumbnailUrl ?: itunesSearch.artworkUrl,
                            localThumbnailPath = finalArtPath,
                            previewUrl = itunesSearch.previewUrl,
                            previewUrlProvider = "Apple Music",
                            localPreviewPath = downloadedPath,
                            links = song.links.copy(appleMusic = itunesSearch.trackViewUrl ?: song.links.appleMusic)
                        )
                        return@withContext Result.success(updated)
                    }
                }

                val deezerSearch = deezerFetcher.searchTrack(query)
                if (deezerSearch != null && deezerSearch.previewUrl != null) {
                    val previewResult = downloadAndCachePreview(deezerSearch.previewUrl, song)
                    if (previewResult.isSuccess) {
                        val downloadedPath = previewResult.getOrThrow()
                        val hash = File(downloadedPath).nameWithoutExtension
                        var artPath = song.localThumbnailPath
                        val artUrl = deezerSearch.coverUrl ?: song.thumbnailUrl
                        if ((artPath == null || !File(artPath).exists()) && artUrl != null) {
                            val artResult = cacheAlbumArt(artUrl, hash)
                            artPath = artResult.getOrNull()
                        }
                        val finalArtPath = artPath ?: song.localThumbnailPath
                        songLibraryDao.updateMetadata(
                            hash = hash,
                            title = song.title,
                            artist = song.artistName,
                            album = song.albumName ?: deezerSearch.albumName,
                            genre = song.genre,
                            localArtPath = finalArtPath
                        )
                        updateMatchingJournalsArtwork(song.title, song.artistName, finalArtPath, downloadedPath)
                        val updated = song.copy(
                            albumName = song.albumName ?: deezerSearch.albumName,
                            thumbnailUrl = song.thumbnailUrl ?: deezerSearch.coverUrl,
                            localThumbnailPath = finalArtPath,
                            previewUrl = deezerSearch.previewUrl,
                            previewUrlProvider = "Deezer",
                            localPreviewPath = downloadedPath,
                            links = song.links.copy(deezer = deezerSearch.trackUrl ?: song.links.deezer)
                        )
                        return@withContext Result.success(updated)
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

    override suspend fun refetchSongDetails(song: SongDetails): Result<SongDetails> = withContext(Dispatchers.IO) {
        try {
            val path = song.localPreviewPath
            val hash = path?.let { File(it).nameWithoutExtension }
            val entity = hash?.let { songLibraryDao.getByHash(it) }
            val url = song.links.spotify
                ?: song.links.appleMusic
                ?: song.links.deezer
                ?: song.links.youtubeMusic
                ?: song.links.youtube
                ?: entity?.sourceUrl
                ?: song.previewUrl

            if (url == null) {
                return@withContext Result.failure(Exception("No URL available to re-fetch details"))
            }

            val freshResult = fetchSongDetails(url)
            if (freshResult.isSuccess) {
                val fresh = freshResult.getOrThrow()
                val updated = song.copy(
                    title = fresh.title.takeIf { it.isNotBlank() } ?: song.title,
                    artistName = fresh.artistName.takeIf { it.isNotBlank() } ?: song.artistName,
                    albumName = fresh.albumName ?: song.albumName,
                    genre = fresh.genre ?: song.genre,
                    thumbnailUrl = fresh.thumbnailUrl ?: song.thumbnailUrl,
                    localThumbnailPath = fresh.localThumbnailPath ?: song.localThumbnailPath,
                    links = fresh.links
                )
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
                Result.success(updated)
            } else {
                Result.failure(freshResult.exceptionOrNull() ?: Exception("Re-fetch failed"))
            }
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