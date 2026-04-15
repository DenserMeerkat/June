package com.denser.june.core.data.repository

import com.denser.june.core.data.mappers.mapSonglinkResponseToSongDetails
import com.denser.june.core.data.remote.SonglinkApiService
import com.denser.june.core.data.remote.SpotifyScraper
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.preferences.PrivacyPreferences
import kotlinx.coroutines.flow.first

class SongRepositoryImpl(
    private val apiService: SonglinkApiService,
    private val spotifyScraper: SpotifyScraper,
    private val privacyPreferences: PrivacyPreferences
) : SongRepository {

    override suspend fun fetchSongDetails(url: String): Result<SongDetails> {
        if (!privacyPreferences.getIsInternetAllowedFlow().first()) {
            return Result.failure(Exception("Internet access restricted"))
        }
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