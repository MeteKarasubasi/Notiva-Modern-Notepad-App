package com.example.multiapp.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {
    @GET("search")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1
    ): Response<List<GeocodingResponse>>
}

data class GeocodingResponse(
    val lat: String,
    val lon: String,
    val display_name: String,
    val type: String
) 