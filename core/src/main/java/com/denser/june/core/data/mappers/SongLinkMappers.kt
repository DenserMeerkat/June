package com.denser.june.core.data.mappers

import com.denser.june.core.data.dto.SongLinkPageData
import com.denser.june.core.domain.model.PlatformLinks
import com.denser.june.core.domain.model.SongDetails

fun mapSongLinkPageDataToSongDetails(pageData: SongLinkPageData): SongDetails {
    return SongDetails(
        title = pageData.title,
        artistName = pageData.artistName,
        thumbnailUrl = pageData.thumbnailUrl,
        links = PlatformLinks(
            spotify = pageData.spotifyUrl,
            appleMusic = pageData.appleMusicUrl,
            youtubeMusic = pageData.youtubeMusicUrl,
            youtube = pageData.youtubeUrl,
            deezer = pageData.deezerUrl,
            tidal = pageData.tidalUrl,
            amazonMusic = pageData.amazonMusicUrl,
            soundcloud = pageData.soundcloudUrl
        )
    )
}
