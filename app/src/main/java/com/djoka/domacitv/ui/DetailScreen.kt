package com.djoka.domacitv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.djoka.domacitv.data.DetailViewModel

@Composable
fun DetailScreen(
    type: String,
    id: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    LaunchedEffect(type, id) {
        viewModel.load(type, id)
    }

    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        if (state.notFound) {
            Text(text = "Nije pronađeno", color = Color.White, modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        // Fanart - 100% ekrana, sve ostalo je prekriveno preko nje (kao referenca)
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black),
                        startY = 200f
                    )
                )
        )

        // Naslov/logo - gore levo preko slike
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

        // Donji red - dugmad / opis / uloge, sve preko slike (kao na referenci)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, bottom = 32.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Dugmad - stubac, jedno ispod drugog (kao "Play Again" / "Added to Watchlist")
            Column(modifier = Modifier.width(220.dp)) {
                Button(
                    onClick = { /* TODO: plejer */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("▶  Pusti", fontWeight = FontWeight.SemiBold)
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

            // Sredina - zanr/godina/trajanje/uzrast + opis
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

            // Uloge - desna kolona (kao "Starring" na referenci)
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
}
