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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openprep.app.data.ProgressRepository
import com.openprep.app.ui.screens.DashboardScreen
import com.openprep.app.ui.screens.QuizScreen
import com.openprep.app.ui.screens.ServerSetupScreen
import com.openprep.app.ui.screens.VideoPlayerScreen
import com.openprep.app.ui.theme.OpenPrepTheme
import com.openprep.app.viewmodel.AppState
import com.openprep.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenPrepTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize our lightweight DataStore repository
    val progressRepo = remember { ProgressRepository(context) }

    NavHost(navController = navController, startDestination = "setup") {
        
        composable("setup") {
            if (uiState is AppState.Success) {
                navController.navigate("dashboard") { popUpTo("setup") { inclusive = true } }
            }
            ServerSetupScreen(uiState = uiState, onConnect = { url -> viewModel.connectToServer(url) })
        }

        composable("dashboard") {
            val state = uiState
            if (state is AppState.Success) {
                DashboardScreen(
                    manifest = state.manifest,
                    progressRepo = progressRepo,
                    currentServerUrl = viewModel.currentServerUrl,
                    onDisconnect = {
                        viewModel.resetSetup()
                        navController.navigate("setup") { popUpTo("dashboard") { inclusive = true } }
                    },
                    onModuleClick = { module ->
                        val fullUrl = if (module.url.startsWith("http")) module.url else "${viewModel.currentServerUrl}/${module.url}"
                        val encodedUrl = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.toString())
                        val encodedTitle = URLEncoder.encode(module.title, StandardCharsets.UTF_8.toString())
                        
                        when (module.type.lowercase()) {
                            "video" -> {
                                // Mark video as complete immediately when opened
                                coroutineScope.launch { progressRepo.markModuleCompleted(module.id) }
                                navController.navigate("videoPlayer/${module.id}/$encodedUrl/$encodedTitle")
                            }
                            "qbank" -> navController.navigate("quiz/${module.id}/$encodedUrl")
                            "pdf" -> {
                                coroutineScope.launch { progressRepo.markModuleCompleted(module.id) }
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(fullUrl), "application/pdf")
                                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                                }
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            }
                        }
                    }
                )
            } else { navController.navigate("setup") }
        }

        composable("videoPlayer/{id}/{url}/{title}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString())
            VideoPlayerScreen(videoUrl = url, title = title, onNavigateBack = { navController.popBackStack() })
        }

        composable("quiz/{id}/{url}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            
            // To save scores, update your QuizScreen to accept the repository and ID, 
            // and call progressRepo.saveModuleScore(id, finalScore) when the quiz finishes!
            QuizScreen(quizUrl = url, onNavigateBack = { navController.popBackStack() })
        }
    }
}
