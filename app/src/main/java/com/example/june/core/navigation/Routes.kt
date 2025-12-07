package com.example.june.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data class Note(val noteId: Long? = null) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object AboutLibraries : Route

    @Serializable
    data object Backup : Route
}