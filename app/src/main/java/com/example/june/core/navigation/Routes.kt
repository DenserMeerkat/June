package com.example.june.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data class Journal(val journalId: Long? = null) : Route

    @Serializable
    data class JournalMedia(val journalId: Long) : Route

    @Serializable
    data class JournalMediaDetail(
        val journalId: Long,
        val initialIndex: Int
    ) : Route

    @Serializable
    data class MediaViewerRoute(
        val mediaPaths: List<String>,
        val initialIndex: Int
    ) : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object AboutLibraries : Route

    @Serializable
    data object Backup : Route
}