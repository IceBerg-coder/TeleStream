package com.telestream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.telestream.app.ui.screens.HomeScreen
import com.telestream.app.ui.screens.PlayerScreen
import com.telestream.app.ui.screens.SettingsScreen
import com.telestream.app.ui.theme.DarkBackground
import com.telestream.app.ui.theme.TeleStreamTheme
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleStreamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    TeleStreamNavigation()
                }
            }
        }
    }
}

@Composable
fun TeleStreamNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onPlayVideo = { streamUrl, title ->
                    val encodedUrl = URLEncoder.encode(streamUrl, StandardCharsets.UTF_8.toString())
                    val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                    navController.navigate("player/$encodedUrl/$encodedTitle")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "player/{streamUrl}/{title}",
            arguments = listOf(
                navArgument("streamUrl") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: "Video Player"

            val decodedUrl = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8.toString())
            val decodedTitle = URLDecoder.decode(rawTitle, StandardCharsets.UTF_8.toString())

            PlayerScreen(
                streamUrl = decodedUrl,
                videoTitle = decodedTitle,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
