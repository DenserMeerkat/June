package com.denser.june.core.domain.repository

import android.net.Uri
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.SongDetails

import com.denser.june.core.domain.model.SongFetchEvent

import kotlinx.coroutines.flow.Flow

interface SongRepository {
    suspend fun fetchSongDetails(url: String): Result<SongDetails>
    fun fetchSongDetailsWithProgress(url: String): Flow<SongFetchEvent>
    suspend fun downloadAndCachePreview(previewUrl: String, songDetails: SongDetails? = null): Result<String>
    suspend fun cacheAlbumArt(thumbnailUrl: String, songHash: String? = null): Result<String>
    suspend fun attachLocalAudio(uri: Uri): Result<SongDetails>
    fun attachLocalAudioWithProgress(uri: Uri): Flow<SongFetchEvent>
    suspend fun removeFromLibrary(song: SongDetails): Result<Unit>
    suspend fun addToLibrary(song: SongDetails): Result<SongDetails>
    suspend fun updateLibrarySongMeta(original: SongDetails, updated: SongDetails): Result<Unit>
    suspend fun refetchSongDetails(song: SongDetails): Result<SongDetails>
    suspend fun cleanupUnreferencedSongMedia(allJournals: List<Journal>)
    fun getLibrarySongs(): Flow<List<SongDetails>>
}
