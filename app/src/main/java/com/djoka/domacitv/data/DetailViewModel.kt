package com.djoka.domacitv.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SeasonItem(
    val seasonNumber: Int,
    val name: String,
    val posterUrl: String?
)

data class EpisodeItem(
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val stillUrl: String?,
    val rating: Double?
)

data class DetailUiState(
    val title: String = "",
    val logoUrl: String? = null,
    val backdropUrl: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val runtimeText: String? = null,
    val ageRating: String? = null,
    val overview: String? = null,
    val cast: String? = null,
    val collectionMovies: List<GridItem> = emptyList(),   // nastavci filma
    val seasons: List<SeasonItem> = emptyList(),          // sezone serije
    val selectedSeason: Int? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false
)

class DetailViewModel : ViewModel() {

    private val tmdb = TmdbApi.create()
    private val apiKey = TmdbApi.key()

    private var resolvedTmdbId: Int? = null
    private var isMovieType: Boolean = true

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState

    fun load(type: String, rawId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            try {
                val isMovie = type == "movie"
                isMovieType = isMovie

                val tmdbId = rawId.toIntOrNull() ?: resolveImdbId(rawId, isMovie)

                if (tmdbId == null) {
                    _uiState.value = DetailUiState(isLoading = false, notFound = true)
                    return@launch
                }
                resolvedTmdbId = tmdbId

                val detail = if (isMovie) tmdb.movieDetail(tmdbId, apiKey) else tmdb.tvDetail(tmdbId, apiKey)

                var overview = detail.overview
                if (overview.isNullOrBlank()) {
                    val fallback = if (isMovie) {
                        tmdb.movieDetail(tmdbId, apiKey, language = "en-US")
                    } else {
                        tmdb.tvDetail(tmdbId, apiKey, language = "en-US")
                    }
                    overview = fallback.overview
                }

                val logo = detail.images?.logos
                    ?.filter { it.file_path.endsWith(".png") }
                    ?.let { logos ->
                        logos.firstOrNull { it.iso_639_1 == "sr" }
                            ?: logos.firstOrNull { it.iso_639_1 == null }
                            ?: logos.firstOrNull { it.iso_639_1 == "en" }
                    }

                val ageRating = if (isMovie) {
                    val countries = detail.release_dates?.results.orEmpty()
                    val rs = countries.firstOrNull { it.iso_3166_1 == "RS" }
                        ?.release_dates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
                    val us = countries.firstOrNull { it.iso_3166_1 == "US" }
                        ?.release_dates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
                    rs ?: us
                } else {
                    val countries = detail.content_ratings?.results.orEmpty()
                    val rs = countries.firstOrNull { it.iso_3166_1 == "RS" }?.rating
                    val us = countries.firstOrNull { it.iso_3166_1 == "US" }?.rating
                    rs ?: us
                }

                val year = (detail.release_date ?: detail.first_air_date)?.take(4)

                val runtimeMinutes = if (isMovie) detail.runtime else detail.episode_run_time?.firstOrNull()
                val runtimeText = runtimeMinutes?.takeIf { it > 0 }?.let { minutes ->
                    val h = minutes / 60
                    val m = minutes % 60
                    if (h > 0) "${h}h ${m}min" else "${m}min"
                }

                val cast = detail.credits?.cast?.take(4)?.joinToString(", ") { it.name }

                // Nastavci filma (ako pripada kolekciji)
                val collectionMovies = if (isMovie && detail.belongs_to_collection != null) {
                    try {
                        tmdb.collection(detail.belongs_to_collection.id, apiKey).parts
                            .filter { it.id != tmdbId }
                            .map {
                                GridItem(
                                    id = it.id.toString(),
                                    type = "movie",
                                    title = it.title ?: "",
                                    posterUrl = it.poster_path?.let { p -> TmdbApi.POSTER_URL + p }
                                )
                            }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()

                // Sezone serije (samo one koje imaju poster)
                val seasons = if (!isMovie) {
                    detail.seasons
                        ?.filter { it.season_number > 0 && it.poster_path != null }
                        ?.map {
                            SeasonItem(
                                seasonNumber = it.season_number,
                                name = it.name ?: "Sezona ${it.season_number}",
                                posterUrl = TmdbApi.POSTER_URL + it.poster_path
                            )
                        } ?: emptyList()
                } else emptyList()

                _uiState.value = DetailUiState(
                    title = detail.title ?: detail.name ?: "",
                    logoUrl = logo?.let { TmdbApi.LOGO_URL + it.file_path },
                    backdropUrl = detail.backdrop_path?.let { TmdbApi.BACKDROP_URL + it },
                    genre = detail.genres?.firstOrNull()?.name,
                    year = year,
                    runtimeText = runtimeText,
                    ageRating = ageRating?.takeIf { it.isNotBlank() },
                    overview = overview,
                    cast = cast?.takeIf { it.isNotBlank() },
                    collectionMovies = collectionMovies,
                    seasons = seasons,
                    isLoading = false
                )

                // Automatski ucitaj epizode prve sezone sa posterom
                if (seasons.isNotEmpty()) {
                    selectSeason(seasons.first().seasonNumber)
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState(isLoading = false, notFound = true)
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        val id = resolvedTmdbId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedSeason = seasonNumber)
            try {
                val srEpisodes = tmdb.tvSeason(id, seasonNumber, apiKey).episodes
                // Ako neka epizoda nema naziv/opis na srpskom, dovucemo engleski kao rezervu
                val needsFallback = srEpisodes.any { it.name.isNullOrBlank() || it.overview.isNullOrBlank() }
                val enEpisodes = if (needsFallback) {
                    try {
                        tmdb.tvSeason(id, seasonNumber, apiKey, language = "en-US").episodes
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()

                val episodes = srEpisodes.map { sr ->
                    val en = enEpisodes.firstOrNull { it.episode_number == sr.episode_number }
                    EpisodeItem(
                        episodeNumber = sr.episode_number,
                        name = sr.name?.takeIf { it.isNotBlank() }
                            ?: en?.name?.takeIf { it.isNotBlank() }
                            ?: "Epizoda ${sr.episode_number}",
                        overview = sr.overview?.takeIf { o -> o.isNotBlank() }
                            ?: en?.overview?.takeIf { o -> o.isNotBlank() },
                        stillUrl = sr.still_path?.let { p -> TmdbApi.STILL_URL + p },
                        rating = sr.vote_average?.takeIf { r -> r > 0 }
                    )
                }
                _uiState.value = _uiState.value.copy(episodes = episodes)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(episodes = emptyList())
            }
        }
    }

    private suspend fun resolveImdbId(imdbId: String, isMovie: Boolean): Int? {
        val find = tmdb.findByImdbId(imdbId, apiKey)
        return if (isMovie) find.movie_results.firstOrNull()?.id else find.tv_results.firstOrNull()?.id
    }
}
