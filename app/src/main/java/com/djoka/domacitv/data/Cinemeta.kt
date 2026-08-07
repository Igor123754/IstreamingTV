package com.djoka.domacitv.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// --- Modeli koji odgovaraju Cinemeta / Stremio addon protokolu ---

data class CatalogResponse(
    val metas: List<MetaPreview> = emptyList()
)

data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null,
    val genres: List<String>? = null,
    val releaseInfo: String? = null
)

// --- Retrofit interfejs za Cinemeta addon (v3-cinemeta.strem.io) ---
interface CinemetaApi {

    // type: "movie" ili "series", catalogId npr. "top", "popular"
    @GET("catalog/{type}/{catalogId}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("catalogId") catalogId: String
    ): CatalogResponse

    companion object {
        private const val BASE_URL = "https://v3-cinemeta.strem.io/"

        fun create(): CinemetaApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(CinemetaApi::class.java)
        }
    }
}
