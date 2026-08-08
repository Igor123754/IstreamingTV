package com.djoka.domacitv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.djoka.domacitv.data.DetailViewModel
import com.djoka.domacitv.data.EpisodeItem
import com.djoka.domacitv.data.GridItem
import com.djoka.domacitv.data.SeasonItem

@Composable
fun DetailScreen(
    type: String,
    id: String,
    onBack: () -> Unit,
    onItemClick: (String, String) -> Unit,
    onPlayClick: (String) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    LaunchedEffect(type, id) {
        viewModel.load(type, id)
    }

    val state by viewModel.uiState.collectAsState()

    // Kad je link pronadjen (bilo da je vec bio spreman ili se resio na klik), navigiraj ka plejeru
    LaunchedEffect(state.playbackUrl) {
        val url = state.playbackUrl
        if (url != null) {
            onPlayClick(url)
            viewModel.consumePlaybackUrl()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        if (state.notFound) {
            Text(text = "Nije pronađeno", color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Hero sekcija - fiksne visine, isti obrazac kao na pocetnoj strani
            Box(modifier = Modifier.fillMaxWidth().height(620.dp)) {
                AsyncImage(
                    model = state.backdropUrl,
                    contentDescription = state.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                                startX = 0f, endX = 900f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Black),
                                startY = 200f
                            )
                        )
                )

                Box(modifier = Modifier.align(Alignment.TopStart).padding(start = 48.dp, top = 40.dp)) {
                    if (state.logoUrl != null) {
                        AsyncImage(
                            model = state.logoUrl,
                            contentDescription = state.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(70.dp).widthIn(max = 380.dp)
                        )
                    } else {
                        Text(text = state.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 48.dp, bottom = 32.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.width(220.dp)) {
                        Button(
                            onClick = { viewModel.onPlayClicked() },
                            enabled = !state.isResolvingStream,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isResolvingStream) {
                                CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text("▶  Pusti", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        state.streamError?.let { err ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = err, color = Color.Red.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { /* TODO: biblioteka */ },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+  Dodaj na listu")
                        }
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infoParts = listOfNotNull(state.genre, state.year, state.runtimeText)
                            if (infoParts.isNotEmpty()) {
                                Text(
                                    text = infoParts.joinToString("   "),
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 13.sp
                                )
                            }
                            state.ageRating?.let { rating ->
                                if (infoParts.isNotEmpty()) Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(text = rating, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        state.overview?.let { overview ->
                            Text(
                                text = overview,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 3
                            )
                        }
                    }

                    state.cast?.let { cast ->
                        Spacer(modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.width(200.dp)) {
                            Text(text = "Uloge", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = cast, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nastavci filma (kolekcija)
            if (state.collectionMovies.isNotEmpty()) {
                PosterRowSection(title = "Nastavci", items = state.collectionMovies, onItemClick = onItemClick)
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Sezone serije
            if (state.seasons.isNotEmpty()) {
                SeasonRowSection(
                    seasons = state.seasons,
                    selectedSeason = state.selectedSeason,
                    onSeasonClick = { viewModel.selectSeason(it) }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Epizode odabrane sezone
            if (state.episodes.isNotEmpty() && state.selectedSeason != null) {
                EpisodeRowSection(
                    episodes = state.episodes,
                    seasonNumber = state.selectedSeason!!,
                    onEpisodeClick = { season, episode -> viewModel.playEpisode(season, episode) }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Overlay preko celog ekrana dok se link resava (samo ako prefetch jos nije gotov)
        if (state.isResolvingStream) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun PosterRowSection(title: String, items: List<GridItem>, onItemClick: (String, String) -> Unit) {
    Column(modifier = Modifier.padding(start = 48.dp)) {
        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(items) { item ->
                var isFocused by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .focusable()
                        .clickable { onItemClick(item.type, item.id) }
                        .onFocusChanged { isFocused = it.isFocused }
                        .scale(if (isFocused) 1.08f else 1f)
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(140.dp)
                            .height(210.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isFocused) Modifier.border(BorderStroke(3.dp, Color.White), RoundedCornerShape(8.dp))
                                else Modifier
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = item.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SeasonRowSection(
    seasons: List<SeasonItem>,
    selectedSeason: Int?,
    onSeasonClick: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(start = 48.dp)) {
        Text(text = "Sezone", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(seasons) { season ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = season.seasonNumber == selectedSeason
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .focusable()
                        .clickable { onSeasonClick(season.seasonNumber) }
                        .onFocusChanged { isFocused = it.isFocused }
                        .scale(if (isFocused) 1.08f else 1f)
                ) {
                    AsyncImage(
                        model = season.posterUrl,
                        contentDescription = season.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(140.dp)
                            .height(210.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                when {
                                    isFocused -> Modifier.border(BorderStroke(3.dp, Color.White), RoundedCornerShape(8.dp))
                                    isSelected -> Modifier.border(BorderStroke(2.dp, Color.White.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                                    else -> Modifier
                                }
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = season.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EpisodeRowSection(episodes: List<EpisodeItem>, seasonNumber: Int, onEpisodeClick: (Int, Int) -> Unit) {
    Column(modifier = Modifier.padding(start = 48.dp)) {
        Text(text = "Epizode", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(episodes) { ep ->
                var isFocused by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .focusable()
                        .clickable { onEpisodeClick(seasonNumber, ep.episodeNumber) }
                        .onFocusChanged { isFocused = it.isFocused }
                        .scale(if (isFocused) 1.04f else 1f)
                ) {
                    AsyncImage(
                        model = ep.stillUrl,
                        contentDescription = ep.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(260.dp)
                            .height(146.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isFocused) Modifier.border(BorderStroke(3.dp, Color.White), RoundedCornerShape(8.dp))
                                else Modifier
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${ep.episodeNumber}. ${ep.name}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        ep.rating?.let { rating ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "★ ${"%.1f".format(rating)}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    ep.overview?.let { overview ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = overview,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
