package com.djoka.domacitv.data

import com.djoka.domacitv.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class TmdbListResponse(val results: List<TmdbListItem> = emptyList())

data class TmdbListItem(
    val id: Int,
    val title: String? = null,      // filmovi
    val name: String? = null,       // serije
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val overview: String? = null
)

data class TmdbGenre(val name: String)
data class TmdbLogo(val file_path: String, val iso_639_1: String? = null)
data class TmdbImages(val logos: List<TmdbLogo> = emptyList())

// --- Uzrasna preporuka (starosno ograničenje) - filmovi ---
data class ReleaseDateInfo(val certification: String? = null)
data class ReleaseDatesCountry(val iso_3166_1: String, val release_dates: List<ReleaseDateInfo> = emptyList())
data class ReleaseDatesWrapper(val results: List<ReleaseDatesCountry> = emptyList())

// --- Uzrasna preporuka - serije ---
data class ContentRatingCountry(val iso_3166_1: String, val rating: String? = null)
data class ContentRatingsWrapper(val results: List<ContentRatingCountry> = emptyList())

data class TmdbDetail(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val backdrop_path: String? = null,
    val genres: List<TmdbGenre>? = null,
    val images: TmdbImages? = null,
    val release_dates: ReleaseDatesWrapper? = null,
    val content_ratings: ContentRatingsWrapper? = null
)

interface TmdbApi {

    @GET("trending/movie/week")
    suspend fun trendingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS"
    ): TmdbListResponse

    @GET("trending/tv/week")
    suspend fun trendingTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS"
    ): TmdbListResponse

    @GET("movie/popular")
    suspend fun popularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS",
        @Query("page") page: Int = 1
    ): TmdbListResponse

    @GET("tv/popular")
    suspend fun popularTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS",
        @Query("page") page: Int = 1
    ): TmdbListResponse

    // append_to_response vraća clearlogo i uzrasnu preporuku u istom pozivu
    @GET("movie/{id}")
    suspend fun movieDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS",
        @Query("append_to_response") append: String = "images,release_dates",
        @Query("include_image_language") includeImageLanguage: String = "sr,en,null"
    ): TmdbDetail

    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS",
        @Query("append_to_response") append: String = "images,content_ratings",
        @Query("include_image_language") includeImageLanguage: String = "sr,en,null"
    ): TmdbDetail

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"
        const val BACKDROP_URL = "https://image.tmdb.org/t/p/original"
        const val POSTER_URL = "https://image.tmdb.org/t/p/w500"
        const val LOGO_URL = "https://image.tmdb.org/t/p/w500"

        fun create(): TmdbApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(TmdbApi::class.java)
        }

        // Ključ dolazi iz GitHub Secret-a (TMDB_API_KEY) preko BuildConfig, ne stoji u kodu
        fun key(): String = BuildConfig.TMDB_API_KEY
    }
}
