package com.djoka.domacitv.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class SubtitleResponse(val subtitles: List<SubtitleItem> = emptyList())

data class SubtitleItem(
    val id: String,
    val url: String,
    val lang: String
)

interface SubtitleAddonApi {

    // id je isti format kao za stream: IMDB id za film, "tt.../sezona/epizoda" za seriju
    @GET("subtitles/{type}/{id}.json")
    suspend fun subtitles(
        @Path("type") type: String,
        @Path("id") id: String
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
