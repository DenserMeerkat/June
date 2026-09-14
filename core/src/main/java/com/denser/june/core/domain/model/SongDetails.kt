package com.denser.june.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SongDetails(
    val title: String,
    val artistName: String,
    val thumbnailUrl: String? = null,
    val previewUrl: String? = null,
    val previewUrlProvider: String? = null,
    val links: PlatformLinks = PlatformLinks()
)

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
