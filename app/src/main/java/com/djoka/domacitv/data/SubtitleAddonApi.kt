package com.djoka.domacitv.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class SubtitleResponse(val subtitles: List<SubtitleItem> = emptyList())

data class SubtitleItem(
    val id: String? = null,
    val url: String? = null,
    val lang: String? = null
)

interface SubtitleAddonApi {

    // id je isti format kao za stream: IMDB id za film, "tt.../sezona/epizoda" za seriju.
    // Bez "extra" - obican lookup po naslovu (moze da vrati titl koji ne odgovara bas ovom fajlu).
    @GET("subtitles/{type}/{id}.json")
    suspend fun subtitles(
        @Path("type") type: String,
        @Path("id") id: String
    ): SubtitleResponse

    // Sa "extra" (videoHash+videoSize) - trazi titl upareno bas sa OVIM tacnim video fajlom,
    // pa je sinhronizacija garantovana kad addon ima hash-matched rezultat.
    @GET("subtitles/{type}/{id}/{extra}.json")
    suspend fun subtitlesWithHash(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("extra") extra: String
    ): SubtitleResponse

    companion object {
        const val OPENSUBTITLES_URL = "https://opensubtitles-v3.strem.io/"

        fun create(baseUrl: String): SubtitleAddonApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(SubtitleAddonApi::class.java)
        }
    }
}
