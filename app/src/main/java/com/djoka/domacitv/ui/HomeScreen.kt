package com.djoka.domacitv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.djoka.domacitv.data.CatalogRow
import com.djoka.domacitv.data.HomeViewModel
import com.djoka.domacitv.data.MetaPreview

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            state.featured?.let { HeroSection(it) }
            Spacer(modifier = Modifier.height(24.dp))
            state.rows.forEach { row ->
                CatalogRowSection(row)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HeroSection(item: MetaPreview) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        AsyncImage(
            model = item.background ?: item.poster,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradijent preko slike - tamniji ka levo/dole radi čitljivosti teksta
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent),
                        startX = 0f,
                        endX = 900f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 300f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 40.dp, end = 400.dp)
        ) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            item.genres?.firstOrNull()?.let { genre ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = genre,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            item.description?.let { desc ->
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
                onClick = { /* TODO: otvori detalje / addon stream picker */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Pusti", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CatalogRowSection(row: CatalogRow) {
    Column(modifier = Modifier.padding(start = 48.dp)) {
        Text(
            text = row.title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 48.dp)
        ) {
            items(row.items) { meta ->
                PosterCard(meta)
            }
        }
    }
}

@Composable
private fun PosterCard(meta: MetaPreview) {
    Column(modifier = Modifier.width(140.dp)) {
        AsyncImage(
            model = meta.poster,
            contentDescription = meta.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(140.dp)
                .height(210.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = meta.name,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}
