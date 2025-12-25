package com.example.june.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SonglinkApiResponse(
    val entityUniqueId: String,
    val userCountry: String,
    val pageUrl: String,
    val entitiesByUniqueId: Map<String, SonglinkEntity>,
    val linksByPlatform: Map<String, SonglinkLink>
)

@Serializable
data class SonglinkEntity(
    val id: String,
    val type: String,
    val title: String? = null,
    val artistName: String? = null,
    val thumbnailUrl: String? = null,
    val thumbnailWidth: Int? = null,
    val thumbnailHeight: Int? = null,
    val apiProvider: String,
    val platforms: List<String>
)

@Serializable
data class SonglinkLink(
    val entityUniqueId: String,
    val url: String,
    val nativeAppUriMobile: String? = null,
    val nativeAppUriDesktop: String? = null
)