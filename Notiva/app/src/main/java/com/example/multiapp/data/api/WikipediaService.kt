package com.example.multiapp.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WikipediaService {
    @GET("page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String,
        @Query("redirect") redirect: Boolean = true
    ): Response<WikipediaResponse>
}

data class WikipediaResponse(
    val title: String,
    val extract: String,
    val extract_html: String,
    val description: String?,
    val thumbnail: WikiThumbnail?
)

data class WikiThumbnail(
    val source: String,
    val width: Int,
    val height: Int
)