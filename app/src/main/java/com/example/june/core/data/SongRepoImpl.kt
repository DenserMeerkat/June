package com.example.june.core.data

import com.example.june.core.data.mappers.mapSonglinkResponseToSongDetails
import com.example.june.core.data.remote.SonglinkApiService
import com.example.june.core.data.remote.SpotifyScraper
import com.example.june.core.domain.SongRepo
import com.example.june.core.domain.data_classes.SongDetails

class SongRepoImpl(
    private val apiService: SonglinkApiService,
    private val spotifyScraper: SpotifyScraper
) : SongRepo {

    override suspend fun fetchSongDetails(url: String): Result<SongDetails> {
        return try {
            val response = apiService.getSongLinks(url)
            var details = mapSonglinkResponseToSongDetails(response)
                ?: return Result.failure(Exception("Could not parse song details"))

            val spotifyId = response.linksByPlatform["spotify"]
                ?.entityUniqueId
                ?.split("::")
                ?.lastOrNull()

            if (spotifyId != null) {
                val previewUrl = spotifyScraper.fetchPreviewUrl(spotifyId)
                details = details.copy(previewUrl = previewUrl)
            }

            Result.success(details)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}