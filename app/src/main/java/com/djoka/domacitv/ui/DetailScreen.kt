package com.djoka.domacitv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }
        if (state.notFound) {
            Text(
                text = "Nije pronađeno",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Backdrop slika sa naslovom preko nje
            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
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
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startY = 250f
                            )
                        )
                )

                if (state.logoUrl != null) {
                    AsyncImage(
                        model = state.logoUrl,
                        contentDescription = state.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 20.dp)
                            .height(70.dp)
                            .widthIn(max = 320.dp)
                    )
                } else {
                    Text(
                        text = state.title,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 20.dp, end = 24.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {

                // Dugmad - Pusti / Dodaj na listu (bez funkcionalnosti dok ne dodamo plejer i biblioteku)
                Row {
                    Button(
                        onClick = { /* TODO: plejer */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("▶  Pusti", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { /* TODO: biblioteka */ },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("+  Dodaj na listu")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Info red - zanr, godina, trajanje, uzrasna preporuka
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infoParts = listOfNotNull(state.genre, state.year, state.runtimeText)
                    if (infoParts.isNotEmpty()) {
                        Text(
                            text = infoParts.joinToString("   "),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                    }
                    state.ageRating?.let { rating ->
                        if (infoParts.isNotEmpty()) Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = rating, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                state.overview?.let { overview ->
                    Text(text = overview, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                state.cast?.let { cast ->
                    Text(text = "Uloge", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = cast, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }
        }
    }
}
