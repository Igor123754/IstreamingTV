package com.djoka.domacitv.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    val isLoading: Boolean = true,
    val notFound: Boolean = false
)

class DetailViewModel : ViewModel() {

    private val tmdb = TmdbApi.create()
    private val apiKey = TmdbApi.key()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState

    fun load(type: String, rawId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            try {
                val isMovie = type == "movie"

                // Addon stavke imaju IMDB id (npr. "tt1234567") - te prvo prevodimo u TMDB id
                val tmdbId = rawId.toIntOrNull() ?: resolveImdbId(rawId, isMovie)

                if (tmdbId == null) {
                    _uiState.value = DetailUiState(isLoading = false, notFound = true)
                    return@launch
                }

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
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState(isLoading = false, notFound = true)
            }
        }
    }

    private suspend fun resolveImdbId(imdbId: String, isMovie: Boolean): Int? {
        val find = tmdb.findByImdbId(imdbId, apiKey)
        return if (isMovie) find.movie_results.firstOrNull()?.id else find.tv_results.firstOrNull()?.id
    }
}
