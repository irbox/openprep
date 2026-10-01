package com.openprep.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
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

    LaunchedEffect(uiState) {
        if (uiState is AppState.Error) {
            Log.e("OpenPrep", "State Error: ${(uiState as AppState.Error).message}")
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
            "video" -> navController.navigate("lectureView/${module.id}/$encodedUrl")
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

        composable("dashboard") {
            if (uiState is AppState.Success) {
                MainAppScreen(
                    manifest = (uiState as AppState.Success).manifest,
                    progressRepo = progressRepo,
                    currentServerUrl = viewModel.currentServerUrl,
                    onDisconnect = { coroutineScope.launch { progressRepo.clearServerUrl() }; viewModel.resetSetup(); navController.navigate("setup") { popUpTo(0) } },
                    onSyncRequested = { viewModel.connectToServer(viewModel.currentServerUrl) },
                    onSearchClick = { navController.navigate("search") },
                    onCustomModuleClick = { navController.navigate("customModule") },
                    onOpenDeck = { deckUrl ->
                        val encoded = URLEncoder.encode(deckUrl, StandardCharsets.UTF_8.toString())
                        navController.navigate("treasures/$encoded")
                    },
                    onSubjectClick = { subjectId -> navController.navigate("subject/$subjectId") },
                    onModuleClick = onModuleClicked
                )
            } else { LaunchedEffect(Unit) { navController.navigate("setup") { popUpTo(0) } } }
        }

        composable("customModule") {
            if (uiState is AppState.Success) {
                CustomModuleScreen(
                    manifest = (uiState as AppState.Success).manifest,
                    progressRepo = progressRepo,
                    onNavigateBack = { navController.popBackStack() },
                    onLaunchCustomTest = { quizUrl ->
                        val encoded = URLEncoder.encode(quizUrl, StandardCharsets.UTF_8.toString())
                        navController.navigate("quiz/custom/$encoded")
                    }
                )
            }
        }

        composable("search") {
            if (uiState is AppState.Success) {
                SearchScreen((uiState as AppState.Success).manifest, progressRepo, viewModel.currentServerUrl, onNavigateBack = { navController.popBackStack() }, onModuleClick = onModuleClicked)
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

        composable("lectureView/{id}/{url}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val rawUrl = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            if (uiState is AppState.Success) {
                val mod = (uiState as AppState.Success).manifest.subjects.flatMap { it.modules }.find { it.id == id }
                if (mod != null) {
                    VideoLectureScreen(
                        module = mod,
                        fullVideoUrl = rawUrl,
                        progressRepo = progressRepo,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenNotes = { notesUrl ->
                            val encNotes = URLEncoder.encode(notesUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("pdfViewer/$encNotes/ClassNotes")
                        },
                        onOpenRelatedQuiz = { quizUrl ->
                            val encQuiz = URLEncoder.encode(quizUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("quiz/related/$encQuiz")
                        }
                    )
                }
            }
        }

        composable("quiz/{id}/{url}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); QuizScreen(quizUrl = url, onNavigateBack = { navController.popBackStack() }) }
        composable("pdfViewer/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); PdfViewerScreen(pdfUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
        composable("webView/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); WebViewScreen(url = url, title = title, onNavigateBack = { navController.popBackStack() }) }
        composable("treasures/{url}") { backStackEntry ->
            val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString())
            TreasuresScreen(url = url, progressRepo = progressRepo, onNavigateBack = { navController.popBackStack() })
        }
        composable("imageViewer/{url}/{title}") { backStackEntry -> val url = URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", StandardCharsets.UTF_8.toString()); val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString()); ImageViewerScreen(imageUrl = url, title = title, onNavigateBack = { navController.popBackStack() }) }
    }
}
