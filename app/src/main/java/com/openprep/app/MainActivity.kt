package com.openprep.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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
            val progressRepo = remember { ProgressRepository(applicationContext) }
            val themeMode by progressRepo.getThemeMode().collectAsState(initial = 0)

            OpenPrepTheme(themeMode = themeMode) { 
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OpenPrepApp(progressRepo)
                }
            }
        }
    }
}

@Composable
fun OpenPrepApp(progressRepo: ProgressRepository, viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val savedServerUrl by progressRepo.getServerUrl().collectAsState(initial = null)
    val hasSeenOnboarding by progressRepo.hasSeenOnboarding().collectAsState(initial = false)

    // VERBOSE DEBUGGING: Log State changes
    LaunchedEffect(uiState) {
        if (uiState is AppState.Error) {
            Log.e("OpenPrep", "App State Error: ${(uiState as AppState.Error).message}")
            Toast.makeText(context, "Connection Error: ${(uiState as AppState.Error).message}", Toast.LENGTH_LONG).show()
        }
    }

    val onModuleClicked: (com.openprep.app.model.Module) -> Unit = { module ->
        val fullUrl = if (module.url.startsWith("http")) module.url else "${viewModel.currentServerUrl}/${module.url}"
        val encodedUrl = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8.toString())
        val encodedTitle = URLEncoder.encode(module.title, StandardCharsets.UTF_8.toString())
        
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

    NavHost(navController = navController, startDestination = "splash") {
        
        composable("splash") {
            SplashScreen(onSplashFinished = {
                if (!hasSeenOnboarding) navController.navigate("intro") { popUpTo(0) }
                else if (!savedServerUrl.isNullOrBlank()) viewModel.connectToServer(savedServerUrl!!)
                else navController.navigate("setup") { popUpTo(0) }
            })
            LaunchedEffect(uiState) {
                if (uiState is AppState.Success) navController.navigate("dashboard") { popUpTo(0) }
                else if (uiState is AppState.Error && hasSeenOnboarding) navController.navigate("setup") { popUpTo(0) }
            }
        }

        composable("intro") {
            IntroScreen(onFinishIntro = { coroutineScope.launch { progressRepo.setOnboardingSeen() }; navController.navigate("setup") { popUpTo(0) } })
        }

        composable("setup") {
            LaunchedEffect(uiState) { if (uiState is AppState.Success) navController.navigate("dashboard") { popUpTo(0) } }
            ServerSetupScreen(uiState = uiState, onConnect = { url -> coroutineScope.launch { progressRepo.saveServerUrl(url) }; viewModel.connectToServer(url) })
        }

        // ... inside MainActivity.kt (Replace just the NavHost routing blocks)
        composable("dashboard") {
            if (uiState is AppState.Success) {
                // FIXED: We removed MainAppScreen wrapper! Native Scaffold used here!
                var selectedTab by remember { mutableStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                            NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.LibraryBooks, "QBank") }, label = { Text("QBank") })
                            NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Bookmark, "Saved") }, label = { Text("Saved") })
                            NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") })
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> DashboardScreen((uiState as AppState.Success).manifest, progressRepo, viewModel.currentServerUrl, { navController.navigate("search") }, { s -> navController.navigate("subject/$s") }, onModuleClicked)
                            // 1 -> QBank Screen (Could just be Subject Screen filtered to "qbank")
                            2 -> SavedScreen((uiState as AppState.Success).manifest, progressRepo, viewModel.currentServerUrl, onModuleClicked)
                            3 -> ProfileScreen(progressRepo, (uiState as AppState.Success).manifest.supportUrl, { viewModel.connectToServer(viewModel.currentServerUrl) }, { viewModel.resetSetup(); navController.navigate("setup") { popUpTo(0) } })
                        }
                    }
                }
            } else { LaunchedEffect(Unit) { navController.navigate("setup") { popUpTo(0) } } }
        }
        
        // ... (Keep other sub-routes)

        // USE SMART VIDEO PLAYER!
        composable("videoPlayer/{id}/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); SmartVideoPlayerScreen(videoUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }

        composable("search") {
            if (uiState is AppState.Success) {
                SearchScreen(manifest = (uiState as AppState.Success).manifest, progressRepo = progressRepo, currentServerUrl = viewModel.currentServerUrl, onNavigateBack = { navController.popBackStack() }, onModuleClick = onModuleClicked)
            }
        }

        composable("subject/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            if (uiState is AppState.Success) {
                val subject = (uiState as AppState.Success).manifest.subjects.find { it.id == subjectId }
                if (subject != null) {
                    SubjectScreen(subject = subject, progressRepo = progressRepo, currentServerUrl = viewModel.currentServerUrl, onNavigateBack = { navController.popBackStack() }, onModuleClick = onModuleClicked)
                }
            }
        }

        composable("videoPlayer/{id}/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); VideoPlayerScreen(videoUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
        composable("quiz/{id}/{url}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); QuizScreen(quizUrl = url, onNavigateBack = { navController.popBackStack() }) }
        composable("pdfViewer/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); PdfViewerScreen(pdfUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
        composable("webView/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); WebViewScreen(url = url, title = title, onNavigateBack = { navController.popBackStack() }) }
        composable("treasures/{url}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); TreasuresScreen(url = url, onNavigateBack = { navController.popBackStack() }) }
        composable("imageViewer/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); ImageViewerScreen(imageUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
    }
}
