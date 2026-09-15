package com.denser.june.core.data.repository

import com.denser.june.core.data.mappers.mapSongLinkPageDataToSongDetails
import com.denser.june.core.data.remote.SongLinkScraper
import com.denser.june.core.data.remote.SpotifyScraper
import com.denser.june.core.data.remote.DeezerFetcher
import com.denser.june.core.data.remote.ItunesFetcher
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.core.domain.model.SongDetails

class SongRepositoryImpl(
    private val songLinkScraper: SongLinkScraper,
    private val spotifyScraper: SpotifyScraper,
    private val deezerFetcher: DeezerFetcher,
    private val itunesFetcher: ItunesFetcher
) : SongRepository {

    override suspend fun fetchSongDetails(url: String): Result<SongDetails> {
        return try {
            val pageData = songLinkScraper.fetchPageData(url)
                ?: return Result.failure(Exception("Could not resolve song details"))

            var details = mapSongLinkPageDataToSongDetails(pageData)

            var previewUrl: String? = null
            var previewProvider: String? = null

            val spotifyId = pageData.spotifyUniqueId?.split("::")?.lastOrNull()
                ?: pageData.spotifyUniqueId?.split("|")?.lastOrNull()

            if (spotifyId != null) {
                previewUrl = spotifyScraper.fetchPreviewUrl(spotifyId)
                if (previewUrl != null) previewProvider = "Spotify"
            }

            if (previewUrl == null) {
                val deezerId = pageData.deezerUniqueId?.split("|")?.lastOrNull()
                if (deezerId != null) {
                    previewUrl = deezerFetcher.fetchPreviewUrl(deezerId)
                    if (previewUrl != null) previewProvider = "Deezer"
                }
            }

            if (previewUrl == null && pageData.appleMusicUrl != null) {
                val appleMusicId = pageData.appleMusicUrl
                    .substringAfterLast("/")
                    .substringBefore("?")
                if (appleMusicId.isNotBlank()) {
                    previewUrl = itunesFetcher.fetchPreviewUrl(appleMusicId)
                    if (previewUrl != null) previewProvider = "Apple Music"
                }
            }

            if (previewUrl != null) {
                details = details.copy(
                    previewUrl = previewUrl,
                    previewUrlProvider = previewProvider
                )
            }

            Result.success(details)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}