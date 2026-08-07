package com.djoka.domacitv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.djoka.domacitv.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DomaciTVApp()
        }
    }
}

@Composable
fun DomaciTVApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        HomeScreen()
    }
}
