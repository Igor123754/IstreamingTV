package com.djoka.domacitv.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HeroItem(
    val id: Int,
    val title: String,
    val genre: String?,
    val overview: String?,
    val backdropUrl: String?,
    val logoUrl: String?
)

data class GridItem(
    val id: Int,
    val title: String,
    val posterUrl: String?
)

data class HomeUiState(
    val heroItems: List<HeroItem> = emptyList(),
    val heroIndex: Int = 0,
    val popularMovies: List<GridItem> = emptyList(),
    val popularSeries: List<GridItem> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel : ViewModel() {

    private val api = TmdbApi.create()
    private val apiKey = TmdbApi.key()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHome()
        rotateHero()
    }

    private fun loadHome() {
        viewModelScope.launch {
            try {
                // Top 10 trending filmova + top 10 trending serija za hero rotaciju
                val trendingMovies = api.trendingMovies(apiKey).results.take(10)
                val trendingSeries = api.trendingTv(apiKey).results.take(10)

                val heroMovies = trendingMovies.map { async { buildHeroItem(it.id, isMovie = true) } }
                val heroSeries = trendingSeries.map { async { buildHeroItem(it.id, isMovie = false) } }
                val heroItems = (heroMovies + heroSeries).awaitAll()

                // Katalozi ispod - najpopularniji filmovi/serije (portret posteri)
                val popularMovies = api.popularMovies(apiKey).results.map {
                    GridItem(
                        id = it.id,
                        title = it.title ?: "",
                        posterUrl = it.poster_path?.let { p -> TmdbApi.POSTER_URL + p }
                    )
                }
                val popularSeries = api.popularTv(apiKey).results.map {
                    GridItem(
                        id = it.id,
                        title = it.name ?: "",
                        posterUrl = it.poster_path?.let { p -> TmdbApi.POSTER_URL + p }
                    )
                }

                _uiState.value = _uiState.value.copy(
                    heroItems = heroItems,
                    popularMovies = popularMovies,
                    popularSeries = popularSeries,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // Vuče detalje + clearlogo na srpskom; ako opis fali na srpskom, dovuče engleski
    private suspend fun buildHeroItem(id: Int, isMovie: Boolean): HeroItem {
        val detail = if (isMovie) api.movieDetail(id, apiKey) else api.tvDetail(id, apiKey)

        var overview = detail.overview
        if (overview.isNullOrBlank()) {
            val fallback = if (isMovie) {
                api.movieDetail(id, apiKey, language = "en-US")
            } else {
                api.tvDetail(id, apiKey, language = "en-US")
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

        return HeroItem(
            id = id,
            title = detail.title ?: detail.name ?: "",
            genre = detail.genres?.firstOrNull()?.name,
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
