package com.djoka.domacitv.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CatalogRow(
    val title: String,
    val items: List<MetaPreview>
)

data class HomeUiState(
    val featured: MetaPreview? = null,
    val rows: List<CatalogRow> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel : ViewModel() {

    private val api = CinemetaApi.create()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHome()
    }

    private fun loadHome() {
        viewModelScope.launch {
            try {
                val popularMovies = api.getCatalog("movie", "top").metas
                val popularSeries = api.getCatalog("series", "top").metas

                val featured = popularMovies.firstOrNull()

                _uiState.value = HomeUiState(
                    featured = featured,
                    rows = listOf(
                        CatalogRow("Popularni filmovi", popularMovies),
                        CatalogRow("Popularne serije", popularSeries)
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
