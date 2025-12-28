package com.example.june.core.domain

import com.example.june.core.domain.data_classes.SongDetails

interface SongRepo {
    suspend fun fetchSongDetails(url: String): Result<SongDetails>
}