package com.denser.june.core.data.mappers

import com.denser.june.core.data.dto.SonglinkApiResponse
import com.denser.june.core.domain.data_classes.PlatformLinks
import com.denser.june.core.domain.data_classes.SongDetails

fun mapSonglinkResponseToSongDetails(apiResponse: SonglinkApiResponse): SongDetails? {
    val mainEntityKey = apiResponse.entityUniqueId
    val mainEntity = apiResponse.entitiesByUniqueId[mainEntityKey] ?: return null
    val linksMap = apiResponse.linksByPlatform

    val platformLinks = PlatformLinks(
        spotify = linksMap["spotify"]?.url,
        appleMusic = linksMap["appleMusic"]?.url ?: linksMap["itunes"]?.url,
        youtubeMusic = linksMap["youtubeMusic"]?.url,
        youtube = linksMap["youtube"]?.url,
        amazonMusic = linksMap["amazonMusic"]?.url,
        deezer = linksMap["deezer"]?.url,
        tidal = linksMap["tidal"]?.url,
        soundcloud = linksMap["soundcloud"]?.url
    )

    return SongDetails(
        title = mainEntity.title ?: "Unknown Title",
        artistName = mainEntity.artistName ?: "Unknown Artist",
        thumbnailUrl = mainEntity.thumbnailUrl,
        links = platformLinks
    )
}