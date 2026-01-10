package com.denser.june.core.data.remote

import com.denser.june.core.data.dto.SonglinkApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SonglinkApiService {
    @GET("v1-alpha.1/links")
    suspend fun getSongLinks(
        @Query("url") url: String,
        @Query("userCountry") userCountry: String = "US",
        @Query("songIfSingle") songIfSingle: Boolean = true
    ): SonglinkApiResponse
}