package com.djoka.domacitv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.djoka.domacitv.data.GridItem
import com.djoka.domacitv.data.HeroItem
import com.djoka.domacitv.data.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val currentHero = state.heroItems.getOrNull(state.heroIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero banner - fanart, ali normalne visine (ne preko celog ekrana)
        Box(modifier = Modifier.fillMaxWidth().height(620.dp)) {
            currentHero?.let { HeroSection(it) }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Katalog stranica - odvojeno filmovi i serije, vertikalni posteri, bez brojeva
        if (state.popularMovies.isNotEmpty()) {
            CatalogRowSection(title = "Najpopularniji filmovi", items = state.popularMovies)
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (state.popularSeries.isNotEmpty()) {
            CatalogRowSection(title = "Najpopularnije serije", items = state.popularSeries)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeroSection(item: HeroItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = item.backdropUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startX = 0f,
                        endX = 1000f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                        startY = 250f
                    )
                )
        )

        // Tekst/logo centriran po visini hero-a (kao na Apple TV+), ne zalepljen za dno
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp, end = 500.dp)
        ) {
            if (item.logoUrl != null) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(90.dp)
                        .widthIn(max = 420.dp)
                )
            } else {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (item.genre != null || item.ageRating != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.genre?.let { genre ->
                        Text(text = genre, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                    item.ageRating?.let { rating ->
                        if (item.genre != null) Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = rating, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
            }

            item.overview?.let { desc ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: detalji + izbor stream-a */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Pusti", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CatalogRowSection(title: String, items: List<GridItem>) {
    Column(modifier = Modifier.padding(start = 48.dp)) {
        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(items) { meta -> PosterCard(meta) }
        }
    }
}

@Composable
private fun PosterCard(item: GridItem) {
    Column(modifier = Modifier.width(140.dp)) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(140.dp)
                .height(210.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = item.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
    }
}
