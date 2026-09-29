package com.openprep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openprep.app.ui.screens.DashboardScreen
import com.openprep.app.ui.screens.ServerSetupScreen
import com.openprep.app.ui.screens.VideoPlayerScreen
import com.openprep.app.ui.theme.OpenPrepTheme
import com.openprep.app.viewmodel.AppState
import com.openprep.app.viewmodel.MainViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenPrepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenPrepApp()
                }
            }
        }
    }
}

@Composable
fun OpenPrepApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "setup") {
        
        // 1. Setup / Loading Screen
        composable("setup") {
            // Automatically skip to dashboard if we already fetched data
            if (uiState is AppState.Success) {
                navController.navigate("dashboard") {
                    popUpTo("setup") { inclusive = true }
                }
            }
            
            ServerSetupScreen(
                uiState = uiState,
                onConnect = { url -> viewModel.connectToServer(url) }
            )
        }

        // 2. Main Dashboard (Course Content)
        composable("dashboard") {
            val state = uiState
            if (state is AppState.Success) {
                DashboardScreen(
                    manifest = state.manifest,
                    onDisconnect = {
                        viewModel.resetSetup()
                        navController.navigate("setup") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    onModuleClick = { module ->
                        if (module.type.lowercase() == "video") {
                            // Construct full URL if it's relative, or use as is if absolute
                            val fullUrl = if (module.url.startsWith("http")) module.url 
                                          else "${viewModel.currentServerUrl}/${module.url}"
                            
                            val encodedUrl = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(module.title, StandardCharsets.UTF_8.toString())
                            
                            navController.navigate("videoPlayer/$encodedUrl/$encodedTitle")
                        } else {
                            // In the future, route to a PDF viewer here!
                            println("Unsupported module type for now: ${module.type}")
                        }
                    }
                )
            } else {
                // Failsafe: if state is lost, go back to setup
                navController.navigate("setup")
            }
        }

        // 3. Video Player Screen
        composable("videoPlayer/{url}/{title}") { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: "Video"
            
            val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.toString())

            VideoPlayerScreen(
                videoUrl = url,
                title = title,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
