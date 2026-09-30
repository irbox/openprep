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
    val savedServerUrl by progressRepo.getServerUrl().collectAsState(initial = null)
    val hasSeenOnboarding by progressRepo.hasSeenOnboarding().collectAsState(initial = false)

    NavHost(navController = navController, startDestination = "splash") {
        
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    if (!hasSeenOnboarding) {
                        navController.navigate("intro") { popUpTo("splash") { inclusive = true } }
                    } else if (!savedServerUrl.isNullOrBlank()) {
                        viewModel.connectToServer(savedServerUrl!!)
                    } else {
                        navController.navigate("setup") { popUpTo("splash") { inclusive = true } }
                    }
                }
            )

            LaunchedEffect(uiState) {
                if (uiState is AppState.Success) {
                    navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                } else if (uiState is AppState.Error && hasSeenOnboarding) {
                    navController.navigate("setup") { popUpTo("splash") { inclusive = true } }
                }
            }
        }

        composable("intro") {
            IntroScreen(
                onFinishIntro = {
                    coroutineScope.launch { progressRepo.setOnboardingSeen() }
                    navController.navigate("setup") { popUpTo("intro") { inclusive = true } }
                }
            )
        }

        composable("setup") {
            LaunchedEffect(uiState) {
                if (uiState is AppState.Success) {
                    navController.navigate("dashboard") { popUpTo("setup") { inclusive = true } }
                }
            }

            ServerSetupScreen(
                uiState = uiState, 
                onConnect = { url -> 
                    coroutineScope.launch { progressRepo.saveServerUrl(url) }
                    viewModel.connectToServer(url) 
                }
            )
        }

        composable("dashboard") {
            if (uiState is AppState.Success) {
                MainAppScreen(
                    manifest = (uiState as AppState.Success).manifest,
                    progressRepo = progressRepo,
                    currentServerUrl = viewModel.currentServerUrl,
                    onDisconnect = {
                        coroutineScope.launch { progressRepo.clearServerUrl() }
                        viewModel.resetSetup()
                        navController.navigate("setup") { popUpTo("dashboard") { inclusive = true } }
                    },
                    onModuleClick = { module ->
                        if (module.type == "downloads") {
                            navController.navigate("downloads")
                            return@MainAppScreen
                        }

                        val fullUrl = if (module.url.startsWith("http")) module.url else "${viewModel.currentServerUrl}/${module.url}"
                        val encodedUrl = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.toString())
                        val encodedTitle = URLEncoder.encode(module.title, StandardCharsets.UTF_8.toString())
                        
                        // NEW: Log it to the timeline history!
                        coroutineScope.launch { 
                            progressRepo.markModuleCompleted(module.id)
                            progressRepo.saveLastPlayedModule(module.id)
                            progressRepo.addHistoryItem(module.title, module.type)
                        }

                        when (module.type.lowercase()) {
                            "video" -> navController.navigate("videoPlayer/${module.id}/$encodedUrl/$encodedTitle")
                            "qbank" -> navController.navigate("quiz/${module.id}/$encodedUrl")
                            "pdf" -> navController.navigate("pdfViewer/$encodedUrl/$encodedTitle")
                            "article" -> navController.navigate("webView/$encodedUrl/$encodedTitle")
                            "treasure" -> navController.navigate("treasures/$encodedUrl")
                            "image" -> navController.navigate("imageViewer/$encodedUrl/$encodedTitle")
                        }
                    }
                )
            } else { 
                LaunchedEffect(Unit) { navController.navigate("setup") }
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
        
        composable("webView/{url}/{title}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString())
            WebViewScreen(url = url, title = title, onNavigateBack = { navController.popBackStack() })
        }

        composable("treasures/{url}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            TreasuresScreen(url = url, onNavigateBack = { navController.popBackStack() })
        }

        composable("imageViewer/{url}/{title}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString())
            ImageViewerScreen(imageUrl = url, title = title, onNavigateBack = { navController.popBackStack() })
        }

        composable("downloads") {
            DownloadsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
