package com.djoka.domacitv.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    val collectionMovies: List<GridItem> = emptyList(),
    val seasons: List<SeasonItem> = emptyList(),
    val selectedSeason: Int? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isResolvingStream: Boolean = false,
    val streamError: String? = null,
    val playbackUrl: String? = null
)

class DetailViewModel : ViewModel() {

    private val tmdb = TmdbApi.create()
    private val apiKey = TmdbApi.key()
    private val addonApi = StremioAddonApi.create(StremioAddonApi.DOMACI_ADDON_URL)
    private val foreignAddonApi = StremioAddonApi.create(StremioAddonApi.FOREIGN_ADDON_URL)

    private var resolvedTmdbId: Int? = null
    private var isMovieType: Boolean = true
    private var imdbId: String? = null
    private var prefetchedMovieCandidates: List<StreamCandidate> = emptyList()

    // Da li addon uopste ima ovu seriju (autoritativan spisak epizoda) ili se oslanjamo na TMDB
    private var usingAddonSeasons: Boolean = false
    private var addonVideosBySeason: Map<Int, List<StremioVideo>> = emptyMap()
    private var fallbackPosterUrl: String? = null

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState

    fun load(type: String, rawId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            prefetchedMovie = null
            imdbId = null
            usingAddonSeasons = false
            addonVideosBySeason = emptyMap()
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

                imdbId = if (isMovie) detail.imdb_id else detail.external_ids?.imdb_id
                fallbackPosterUrl = detail.backdrop_path?.let { TmdbApi.BACKDROP_URL + it }

                var seasons: List<SeasonItem> = emptyList()

                if (!isMovie) {
                    // 1) Prvo probaj addon - ako ga ima, on je autoritativan (zna tacno sta je playable)
                    if (imdbId != null) {
                        try {
                            val addonVideos = addonApi.meta("series", imdbId!!).meta?.videos.orEmpty()
                            val bySeasonAddon = addonVideos
                                .filter { it.season != null && it.episode != null }
                                .groupBy { it.season!! }

                            if (bySeasonAddon.isNotEmpty()) {
                                addonVideosBySeason = bySeasonAddon
                                usingAddonSeasons = true
                                seasons = bySeasonAddon.keys.sorted().map { seasonNum ->
                                    val tmdbPoster = detail.seasons
                                        ?.firstOrNull { it.season_number == seasonNum }
                                        ?.poster_path
                                    SeasonItem(
                                        seasonNumber = seasonNum,
                                        name = "Sezona $seasonNum",
                                        posterUrl = tmdbPoster?.let { TmdbApi.POSTER_URL + it } ?: fallbackPosterUrl
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // Nastavljamo na TMDB fallback ispod
                        }
                    }

                    // 2) Addon nema ovu seriju (ili poziv nije uspeo) - prikazi je i dalje preko TMDB podataka
                    if (seasons.isEmpty()) {
                        usingAddonSeasons = false
                        seasons = detail.seasons
                            ?.filter { it.season_number > 0 }
                            ?.map {
                                SeasonItem(
                                    seasonNumber = it.season_number,
                                    name = it.name ?: "Sezona ${it.season_number}",
                                    posterUrl = it.poster_path?.let { p -> TmdbApi.POSTER_URL + p } ?: fallbackPosterUrl
                                )
                            } ?: emptyList()
                    }
                }

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

                if (seasons.isNotEmpty()) {
                    selectSeason(seasons.first().seasonNumber)
                }

                if (isMovie && imdbId != null) {
                    launch {
                        try {
                            prefetchedMovieCandidates = fetchCandidates("movie", imdbId!!)
                        } catch (e: Exception) {
                            // Probace ponovo na klik
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState(isLoading = false, notFound = true)
            }
        }
    }

    // Epizode odabrane sezone. Ako addon ima seriju - lista dolazi od njega (autoritativno).
    // Ako nema - koristi se cela TMDB lista epizoda (samo za pregled, "Pusti" nece naci link).
    fun selectSeason(seasonNumber: Int) {
        val tmdbId = resolvedTmdbId
        _uiState.value = _uiState.value.copy(selectedSeason = seasonNumber)

        viewModelScope.launch {
            var tmdbEpisodes: List<TmdbEpisode> = emptyList()
            var tmdbEpisodesEn: List<TmdbEpisode> = emptyList()
            if (tmdbId != null) {
                try {
                    tmdbEpisodes = tmdb.tvSeason(tmdbId, seasonNumber, apiKey).episodes
                    if (tmdbEpisodes.any { it.name.isNullOrBlank() || it.overview.isNullOrBlank() }) {
                        tmdbEpisodesEn = try {
                            tmdb.tvSeason(tmdbId, seasonNumber, apiKey, language = "en-US").episodes
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                } catch (e: Exception) {
                    // Nema TMDB podataka za ovu sezonu
                }
            }

            val episodeNumbers = if (usingAddonSeasons) {
                addonVideosBySeason[seasonNumber]?.mapNotNull { it.episode }?.sorted() ?: emptyList()
            } else {
                tmdbEpisodes.map { it.episode_number }.sorted()
            }

            val episodes = episodeNumbers.map { epNum ->
                val sr = tmdbEpisodes.firstOrNull { it.episode_number == epNum }
                val en = tmdbEpisodesEn.firstOrNull { it.episode_number == epNum }
                EpisodeItem(
                    episodeNumber = epNum,
                    name = sr?.name?.takeIf { it.isNotBlank() }
                        ?: en?.name?.takeIf { it.isNotBlank() }
                        ?: "Epizoda $epNum",
                    overview = sr?.overview?.takeIf { it.isNotBlank() }
                        ?: en?.overview?.takeIf { it.isNotBlank() },
                    stillUrl = sr?.still_path?.let { TmdbApi.STILL_URL + it },
                    rating = sr?.vote_average?.takeIf { it > 0 }
                )
            }
            _uiState.value = _uiState.value.copy(episodes = episodes)
        }
    }

    fun onPlayClicked() {
        val imdb = imdbId
        if (imdb == null) {
            _uiState.value = _uiState.value.copy(streamError = "Link za ovaj naslov nije pronađen")
            return
        }

        if (isMovieType) {
            val cached = prefetchedMovieCandidates
            if (cached.isNotEmpty()) {
                PlaybackQueue.candidates = cached
                _uiState.value = _uiState.value.copy(playbackUrl = cached.first().url)
                return
            }
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isResolvingStream = true, streamError = null)
                try {
                    val list = fetchCandidates("movie", imdb)
                    if (list.isNotEmpty()) {
                        PlaybackQueue.candidates = list
                        _uiState.value = _uiState.value.copy(isResolvingStream = false, playbackUrl = list.first().url)
                    } else {
                        _uiState.value = _uiState.value.copy(isResolvingStream = false, streamError = "Link nije pronađen")
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isResolvingStream = false, streamError = "Greška pri pretrazi linka")
                }
            }
        } else {
            val season = _uiState.value.selectedSeason ?: _uiState.value.seasons.firstOrNull()?.seasonNumber
            val firstEpisode = _uiState.value.episodes.firstOrNull()?.episodeNumber
            if (season == null || firstEpisode == null) {
                _uiState.value = _uiState.value.copy(streamError = "Nema dostupnih epizoda")
            } else {
                playEpisode(season, firstEpisode)
            }
        }
    }

    fun playEpisode(seasonNumber: Int, episodeNumber: Int) {
        val imdb = imdbId
        if (imdb == null) {
            _uiState.value = _uiState.value.copy(streamError = "Link za ovaj naslov nije pronađen")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResolvingStream = true, streamError = null)
            try {
                val streamId = "$imdb:$seasonNumber:$episodeNumber"
                val list = fetchCandidates("series", streamId)
                if (list.isNotEmpty()) {
                    PlaybackQueue.candidates = list
                    _uiState.value = _uiState.value.copy(isResolvingStream = false, playbackUrl = list.first().url)
                } else {
                    _uiState.value = _uiState.value.copy(isResolvingStream = false, streamError = "Link nije pronađen za ovu epizodu")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isResolvingStream = false, streamError = "Greška pri pretrazi linka")
            }
        }
    }

    // Pita OBA addon-a paralelno i vraca sve validne linkove (sve mirror-e/kvalitete) kao listu -
    // plejer ce sam redom probati dok jedan ne uspe, korisnik to ne vidi.
    private suspend fun fetchCandidates(type: String, streamId: String): List<StreamCandidate> = coroutineScope {
        val domestic = async {
            try { addonApi.stream(type, streamId).streams } catch (e: Exception) { emptyList() }
        }
        val foreign = async {
            try { foreignAddonApi.stream(type, streamId).streams } catch (e: Exception) { emptyList() }
        }
        (domestic.await() + foreign.await())
            .filter { !it.url.isNullOrBlank() }
            .map { StreamCandidate(it.url!!, it.behaviorHints?.headers) }
    }

    fun consumePlaybackUrl() {
        _uiState.value = _uiState.value.copy(playbackUrl = null)
    }

    fun consumeStreamError() {
        _uiState.value = _uiState.value.copy(streamError = null)
    }

    private suspend fun resolveImdbId(imdbId: String, isMovie: Boolean): Int? {
        val find = tmdb.findByImdbId(imdbId, apiKey)
        return if (isMovie) find.movie_results.firstOrNull()?.id else find.tv_results.firstOrNull()?.id
    }
}
