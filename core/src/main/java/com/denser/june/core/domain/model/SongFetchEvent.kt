package com.denser.june.core.domain.model

sealed interface SongFetchEvent {
    data class Progress(val step: Int, val totalSteps: Int, val label: String) : SongFetchEvent
    data class Success(val details: SongDetails) : SongFetchEvent
    data class Error(val exception: Exception) : SongFetchEvent
}
