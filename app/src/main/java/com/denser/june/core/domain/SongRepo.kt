package com.denser.june.core.domain

import com.denser.june.core.domain.data_classes.SongDetails

interface SongRepo {
    suspend fun fetchSongDetails(url: String): Result<SongDetails>
}