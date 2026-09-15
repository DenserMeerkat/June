package com.denser.june.core.data.dto

data class SongLinkPageData(
    val title: String,
    val artistName: String,
    val thumbnailUrl: String?,
    val pageUrl: String,
    val spotifyUrl: String?,
    val spotifyUniqueId: String?,
    val appleMusicUrl: String?,
    val youtubeMusicUrl: String?,
    val youtubeUrl: String?,
    val deezerUrl: String?,
    val deezerUniqueId: String?,
    val tidalUrl: String?,
    val amazonMusicUrl: String?,
    val soundcloudUrl: String?
)
