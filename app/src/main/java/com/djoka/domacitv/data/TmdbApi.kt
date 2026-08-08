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

// --- Glumci (za stranicu sa detaljima) ---
data class TmdbCastMember(val name: String)
data class TmdbCredits(val cast: List<TmdbCastMember> = emptyList())

// --- Kolekcija (nastavci filma) ---
data class TmdbCollectionRef(val id: Int, val name: String? = null)
data class TmdbCollectionResponse(val parts: List<TmdbListItem> = emptyList())

// --- Sezone serije ---
data class TmdbSeason(
    val season_number: Int,
    val name: String? = null,
    val poster_path: String? = null
)

// --- Epizode sezone ---
data class TmdbEpisode(
    val episode_number: Int,
    val name: String? = null,
    val overview: String? = null,
    val still_path: String? = null,
    val vote_average: Double? = null
)
data class TmdbSeasonDetail(val episodes: List<TmdbEpisode> = emptyList())

data class TmdbDetail(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val backdrop_path: String? = null,
    val genres: List<TmdbGenre>? = null,
    val images: TmdbImages? = null,
    val release_dates: ReleaseDatesWrapper? = null,
    val content_ratings: ContentRatingsWrapper? = null,
    val credits: TmdbCredits? = null,
    val runtime: Int? = null,                    // filmovi - trajanje u minutima
    val episode_run_time: List<Int>? = null,      // serije - trajanje epizode
    val release_date: String? = null,             // filmovi - "2023-05-31"
    val first_air_date: String? = null,           // serije
    val belongs_to_collection: TmdbCollectionRef? = null,  // filmovi - nastavci
    val seasons: List<TmdbSeason>? = null,                 // serije
    val imdb_id: String? = null,                  // filmovi - IMDB id (za pretragu stream linka)
    val external_ids: TmdbExternalIds? = null      // serije - odavde vadimo IMDB id
)

data class TmdbExternalIds(val imdb_id: String? = null)

// --- Rezultat pretrage po IMDB id-u (za stavke koje dolaze sa Stremio addon-a) ---
data class TmdbFindResponse(
    val movie_results: List<TmdbListItem> = emptyList(),
    val tv_results: List<TmdbListItem> = emptyList()
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

    // append_to_response vraća clearlogo, uzrasnu preporuku i glumce u istom pozivu
    @GET("movie/{id}")
    suspend fun movieDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS",
        @Query("append_to_response") append: String = "images,release_dates,credits",
        @Query("include_image_language") includeImageLanguage: String = "sr,en,null"
    ): TmdbDetail

    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS",
        @Query("append_to_response") append: String = "images,content_ratings,credits,external_ids",
        @Query("include_image_language") includeImageLanguage: String = "sr,en,null"
    ): TmdbDetail

    // Za stavke sa Stremio addon-a koje imaju IMDB id (npr. "tt1234567") umesto TMDB id-a
    @GET("find/{externalId}")
    suspend fun findByImdbId(
        @Path("externalId") imdbId: String,
        @Query("api_key") apiKey: String,
        @Query("external_source") externalSource: String = "imdb_id"
    ): TmdbFindResponse

    // Svi filmovi iz iste kolekcije (nastavci)
    @GET("collection/{id}")
    suspend fun collection(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS"
    ): TmdbCollectionResponse

    // Epizode jedne sezone serije
    @GET("tv/{id}/season/{seasonNumber}")
    suspend fun tvSeason(
        @Path("id") id: Int,
        @Path("seasonNumber") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "sr-RS"
    ): TmdbSeasonDetail

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"
        const val BACKDROP_URL = "https://image.tmdb.org/t/p/original"
        const val POSTER_URL = "https://image.tmdb.org/t/p/w500"
        const val LOGO_URL = "https://image.tmdb.org/t/p/w500"
        const val STILL_URL = "https://image.tmdb.org/t/p/w300"

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
