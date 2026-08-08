package com.djoka.domacitv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.djoka.domacitv.ui.DetailScreen
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
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(onItemClick = { type, id ->
                    navController.navigate("detail/$type/$id")
                })
            }
            composable(
                route = "detail/{type}/{id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: ""
                val id = backStackEntry.arguments?.getString("id") ?: ""
                DetailScreen(
                    type = type,
                    id = id,
                    onBack = { navController.popBackStack() },
                    onItemClick = { itemType, itemId ->
                        navController.navigate("detail/$itemType/$itemId")
                    }
                )
            }
        }
    }
}
