package com.djoka.domacitv.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// --- Stremio addon protokol (isti za tvoj Balkanteka2/Domaći filmovi addon i bilo koji drugi) ---

data class StremioManifest(
    val id: String? = null,
    val name: String? = null,
    val catalogs: List<StremioManifestCatalog> = emptyList()
)

data class StremioManifestCatalog(
    val id: String,
    val type: String,       // "movie" ili "series"
    val name: String? = null
)

data class StremioCatalogResponse(val metas: List<StremioMeta> = emptyList())

data class StremioMeta(
    val id: String,
    val type: String? = null,
    val name: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null
)

// --- Stream odgovor (link za puštanje) ---
data class StremioStreamResponse(val streams: List<StremioStream> = emptyList())

data class StremioStream(
    val url: String? = null,
    val title: String? = null,
    val name: String? = null
)

interface StremioAddonApi {

    @GET("manifest.json")
    suspend fun manifest(): StremioManifest

    @GET("catalog/{type}/{id}.json")
    suspend fun catalog(
        @Path("type") type: String,
        @Path("id") id: String
    ): StremioCatalogResponse

    // id je IMDB id za film (npr. "tt1234567"), a za epizodu serije "tt1234567:1:2" (sezona:epizoda)
    @GET("stream/{type}/{id}.json")
    suspend fun stream(
        @Path("type") type: String,
        @Path("id") id: String
    ): StremioStreamResponse

    companion object {
        // Tvoj addon sa domaćim filmovima/serijama sortiranim po žanru
        const val DOMACI_ADDON_URL = "https://domaci-filmovi-addon.vercel.app/"

        fun create(baseUrl: String): StremioAddonApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(StremioAddonApi::class.java)
        }
    }
}
