package com.openprep.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
                OpenPrepApp(progressRepo)
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

    // Determine when to show the bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = listOf("dashboard", "bookmarks", "stats", "profile")
    val showBottomBar = currentRoute in bottomBarRoutes

    // Centralized Navigation Logic
    val onModuleClicked: (com.openprep.app.model.Module) -> Unit = { module ->
        if (module.type == "downloads") {
            navController.navigate("downloads")
        } else {
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
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val tabs = listOf(
                        Triple("dashboard", "Home", Icons.Default.Home),
                        Triple("bookmarks", "Bookmarks", Icons.Default.Favorite),
                        Triple("stats", "Progress", Icons.Default.BarChart),
                        Triple("profile", "Profile", Icons.Default.Person)
                    )
                    tabs.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    // Pop up to the start destination of the graph to avoid building up a large stack
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
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

            // --- MAIN TABS ---
            composable("dashboard") {
                if (uiState is AppState.Success) {
                    DashboardScreen(
                        manifest = (uiState as AppState.Success).manifest,
                        progressRepo = progressRepo, currentServerUrl = viewModel.currentServerUrl,
                        onSearchClick = { navController.navigate("search") },
                        onSubjectClick = { subjectId -> navController.navigate("subject/$subjectId") },
                        onModuleClick = onModuleClicked
                    )
                } else { LaunchedEffect(Unit) { navController.navigate("setup") { popUpTo(0) } } }
            }

            composable("bookmarks") {
                if (uiState is AppState.Success) {
                    BookmarksScreen(manifest = (uiState as AppState.Success).manifest, progressRepo = progressRepo, currentServerUrl = viewModel.currentServerUrl, onModuleClick = onModuleClicked)
                }
            }

            composable("stats") {
                if (uiState is AppState.Success) {
                    StatsScreen(manifest = (uiState as AppState.Success).manifest, progressRepo = progressRepo)
                }
            }

            composable("profile") {
                if (uiState is AppState.Success) {
                    ProfileScreen(
                        progressRepo = progressRepo, 
                        supportUrl = (uiState as AppState.Success).manifest.supportUrl,
                        onNavigateToDownloads = { navController.navigate("downloads") }, 
                        onSyncRequested = { viewModel.connectToServer(viewModel.currentServerUrl) },
                        onDisconnect = { coroutineScope.launch { progressRepo.clearServerUrl() }; viewModel.resetSetup(); navController.navigate("setup") { popUpTo(0) } }
                    )
                }
            }

            // --- SUB SCREENS ---
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

            // Viewers
            composable("videoPlayer/{id}/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); VideoPlayerScreen(videoUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
            composable("quiz/{id}/{url}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); QuizScreen(quizUrl = url, onNavigateBack = { navController.popBackStack() }) }
            composable("pdfViewer/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); PdfViewerScreen(pdfUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
            composable("webView/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); WebViewScreen(url = url, title = title, onNavigateBack = { navController.popBackStack() }) }
            composable("treasures/{url}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); TreasuresScreen(url = url, onNavigateBack = { navController.popBackStack() }) }
            composable("imageViewer/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); ImageViewerScreen(imageUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
            composable("downloads") { DownloadsScreen(onNavigateBack = { navController.popBackStack() }) }
        }
    }
}
