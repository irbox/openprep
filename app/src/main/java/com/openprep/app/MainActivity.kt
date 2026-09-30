package com.openprep.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openprep.app.ui.screens.DashboardScreen
import com.openprep.app.ui.screens.QuizScreen
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
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "setup") {
        
        composable("setup") {
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
                        val fullUrl = if (module.url.startsWith("http")) module.url 
                                      else "${viewModel.currentServerUrl}/${module.url}"
                        
                        when (module.type.lowercase()) {
                            "video" -> {
                                val encodedUrl = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.toString())
                                val encodedTitle = URLEncoder.encode(module.title, StandardCharsets.UTF_8.toString())
                                navController.navigate("videoPlayer/$encodedUrl/$encodedTitle")
                            }
                            "qbank" -> {
                                val encodedUrl = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.toString())
                                navController.navigate("quiz/$encodedUrl")
                            }
                            "pdf" -> {
                                // Lightweight approach: Let the OS handle the PDF
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(Uri.parse(fullUrl), "application/pdf")
                                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                                
                                // Wrap in try-catch in case they don't have a PDF viewer installed
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // You could add a Toast here stating "No PDF viewer found"
                                }
                            }
                        }
                    }
                )
            } else {
                navController.navigate("setup")
            }
        }

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

        composable("quiz/{url}") { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

            QuizScreen(
                quizUrl = url,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
