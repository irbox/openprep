package com.openprep.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openprep.app.data.ProgressRepository
import com.openprep.app.ui.screens.*
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
    
    val progressRepo = remember { ProgressRepository(context) }
    
    // Auto-Login Logic
    val savedServerUrl by progressRepo.getServerUrl().collectAsState(initial = null)
    var hasAttemptedAutoLogin by remember { mutableStateOf(false) }

    // If we find a saved URL on startup, connect automatically!
    LaunchedEffect(savedServerUrl) {
        if (savedServerUrl != null && !hasAttemptedAutoLogin && uiState is AppState.Setup) {
            hasAttemptedAutoLogin = true
            viewModel.connectToServer(savedServerUrl!!)
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    // Check if we are already logged in via Auto-login
                    if (uiState is AppState.Success) {
                        navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                    } else {
                        navController.navigate("setup") { popUpTo("splash") { inclusive = true } }
                    }
                }
            )
        }

        composable("setup") {
            if (uiState is AppState.Success) {
                navController.navigate("dashboard") { popUpTo("setup") { inclusive = true } }
            }
            ServerSetupScreen(
                uiState = uiState, 
                onConnect = { url -> 
                    coroutineScope.launch { progressRepo.saveServerUrl(url) } // Save for next time!
                    viewModel.connectToServer(url) 
                }
            )
        }

        composable("dashboard") {
            val state = uiState
            if (state is AppState.Success) {
                MainAppScreen(
                    manifest = state.manifest,
                    progressRepo = progressRepo,
                    currentServerUrl = viewModel.currentServerUrl,
                    onDisconnect = {
                        coroutineScope.launch { progressRepo.clearServerUrl() } // Wipe URL
                        hasAttemptedAutoLogin = false
                        viewModel.resetSetup()
                        navController.navigate("setup") { popUpTo("dashboard") { inclusive = true } }
                    },
                    onModuleClick = { module ->
                        val fullUrl = if (module.url.startsWith("http")) module.url else "${viewModel.currentServerUrl}/${module.url}"
                        val encodedUrl = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.toString())
                        val encodedTitle = URLEncoder.encode(module.title, StandardCharsets.UTF_8.toString())
                        
                        // Save as Last Played
                        coroutineScope.launch { 
                            progressRepo.markModuleCompleted(module.id)
                            progressRepo.saveLastPlayedModule(module.id)
                        }

                        when (module.type.lowercase()) {
                            "video" -> navController.navigate("videoPlayer/${module.id}/$encodedUrl/$encodedTitle")
                            "qbank" -> navController.navigate("quiz/${module.id}/$encodedUrl")
                            "pdf", "article" -> navController.navigate("pdfViewer/$encodedUrl/$encodedTitle")
                        }
                    }
                )
            } else { 
                navController.navigate("setup") 
            }
        }

        composable("videoPlayer/{id}/{url}/{title}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString())
            VideoPlayerScreen(videoUrl = url, title = title, onNavigateBack = { navController.popBackStack() })
        }

        composable("quiz/{id}/{url}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            QuizScreen(quizUrl = url, onNavigateBack = { navController.popBackStack() })
        }

        composable("pdfViewer/{url}/{title}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString())
            PdfViewerScreen(pdfUrl = url, title = title, onNavigateBack = { navController.popBackStack() })
        }
    }
}
