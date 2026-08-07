package com.djoka.domacitv.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HeroItem(
    val id: Int,
    val title: String,
    val genre: String?,
    val ageRating: String?,
    val overview: String?,
    val backdropUrl: String?,
    val logoUrl: String?
)

data class GridItem(
    val id: String,
    val title: String,
    val posterUrl: String?
)

data class CatalogRow(
    val title: String,
    val items: List<GridItem>
)

data class HomeUiState(
    val heroItems: List<HeroItem> = emptyList(),
    val heroIndex: Int = 0,
    val popularMovies: List<GridItem> = emptyList(),
    val popularSeries: List<GridItem> = emptyList(),
    val addonCatalogs: List<CatalogRow> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel : ViewModel() {

    private val tmdb = TmdbApi.create()
    private val apiKey = TmdbApi.key()
    private val addonApi = StremioAddonApi.create(StremioAddonApi.DOMACI_ADDON_URL)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHome()
        rotateHero()
    }

    private fun loadHome() {
        viewModelScope.launch {
            try {
                // Top 10 trending filmova + top 10 trending serija za hero rotaciju (TMDB)
                val trendingMovies = tmdb.trendingMovies(apiKey).results.take(10)
                val trendingSeries = tmdb.trendingTv(apiKey).results.take(10)

                val heroMovies = trendingMovies.map { async { buildHeroItem(it.id, isMovie = true) } }
                val heroSeries = trendingSeries.map { async { buildHeroItem(it.id, isMovie = false) } }
                val heroItems = (heroMovies + heroSeries).awaitAll()

                // Najpopularniji filmovi/serije - TMDB
                val popularMovies = tmdb.popularMovies(apiKey).results.map {
                    GridItem(
                        id = it.id.toString(),
                        title = it.title ?: "",
                        posterUrl = it.poster_path?.let { p -> TmdbApi.POSTER_URL + p }
                    )
                }
                val popularSeries = tmdb.popularTv(apiKey).results.map {
                    GridItem(
                        id = it.id.toString(),
                        title = it.name ?: "",
                        posterUrl = it.poster_path?.let { p -> TmdbApi.POSTER_URL + p }
                    )
                }

                // Katalozi sa tvog Stremio addon-a (žanrovski, filmovi i serije)
                val addonCatalogs = try {
                    loadAddonCatalogs()
                } catch (e: Exception) {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    heroItems = heroItems,
                    popularMovies = popularMovies,
                    popularSeries = popularSeries,
                    addonCatalogs = addonCatalogs,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Cita manifest addon-a i sam otkriva sve njegove kataloge (zanrovi za filmove i serije)
    private suspend fun loadAddonCatalogs(): List<CatalogRow> = coroutineScope {
        val manifest = addonApi.manifest()
        val relevantCatalogs = manifest.catalogs.filter { it.type == "movie" || it.type == "series" }

        val rows = relevantCatalogs.map { cat ->
            async {
                try {
                    val metas = addonApi.catalog(cat.type, cat.id).metas
                    if (metas.isEmpty()) return@async null
                    CatalogRow(
                        title = cat.name ?: cat.id,
                        items = metas.map { meta ->
                            GridItem(
                                id = meta.id,
                                title = meta.name ?: "",
                                posterUrl = meta.poster
                            )
                        }
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll()

        rows.filterNotNull()
    }

    // Vuče detalje + clearlogo + uzrasnu preporuku na srpskom; ako opis fali na srpskom, dovuče engleski
    private suspend fun buildHeroItem(id: Int, isMovie: Boolean): HeroItem {
        val detail = if (isMovie) tmdb.movieDetail(id, apiKey) else tmdb.tvDetail(id, apiKey)

        var overview = detail.overview
        if (overview.isNullOrBlank()) {
            val fallback = if (isMovie) {
                tmdb.movieDetail(id, apiKey, language = "en-US")
            } else {
                tmdb.tvDetail(id, apiKey, language = "en-US")
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

        return HeroItem(
            id = id,
            title = detail.title ?: detail.name ?: "",
            genre = detail.genres?.firstOrNull()?.name,
            ageRating = ageRating?.takeIf { it.isNotBlank() },
            overview = overview,
            backdropUrl = detail.backdrop_path?.let { TmdbApi.BACKDROP_URL + it },
            logoUrl = logo?.let { TmdbApi.LOGO_URL + it.file_path }
        )
    }

    // Menja hero naslov na svakih 5 sekundi
    private fun rotateHero() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                val items = _uiState.value.heroItems
                if (items.isNotEmpty()) {
                    val next = (_uiState.value.heroIndex + 1) % items.size
                    _uiState.value = _uiState.value.copy(heroIndex = next)
                }
            }
        }
    }
}
