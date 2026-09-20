package com.denser.june.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SongDetails(
    val title: String,
    val artistName: String,
    val albumName: String? = null,
    val genre: String? = null,
    val thumbnailUrl: String? = null,
    val localThumbnailPath: String? = null,
    val previewUrl: String? = null,
    val previewUrlProvider: String? = null,
    val localPreviewPath: String? = null,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long? = null,
    val sourceType: SongSourceType = SongSourceType.LINK,
    val links: PlatformLinks = PlatformLinks()
)

@Serializable
enum class SongSourceType { LINK, LOCAL_FILE }

@Serializable
data class PlatformLinks(
    val spotify: String? = null,
    val appleMusic: String? = null,
    val youtubeMusic: String? = null,
    val youtube: String? = null,
    val amazonMusic: String? = null,
    val deezer: String? = null,
    val tidal: String? = null,
    val soundcloud: String? = null
)
