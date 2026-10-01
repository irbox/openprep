package com.openprep.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.Module
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLectureScreen(
    module: Module,
    fullVideoUrl: String,
    progressRepo: ProgressRepository,
    onNavigateBack: () -> Unit,
    onOpenNotes: (String) -> Unit,
    onOpenRelatedQuiz: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()

    val isExternalVideo = fullVideoUrl.contains("youtube.com") || fullVideoUrl.contains("youtu.be") || fullVideoUrl.contains("dailymotion.com") || fullVideoUrl.contains("vimeo.com")

    val isBookmarked by progressRepo.getBookmarkedModules().collectAsState(initial = emptySet())
    val isCompleted by progressRepo.isModuleCompleted(module.id).collectAsState(initial = false)

    val playbackSpeeds = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    var currentSpeedIndex by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Subtopics, 1=Notes, 2=Related QBank

    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(isLandscape) {
        val window = activity?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isLandscape) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            if (!isExternalVideo) {
                setMediaItem(MediaItem.fromUri(fullVideoUrl))
                prepare()
                playWhenReady = true
            }
        }
    }

    LaunchedEffect(currentSpeedIndex) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeeds[currentSpeedIndex])
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
            else if (event == Lifecycle.Event.ON_RESUME) exoPlayer.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = { Text(text = module.title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!isExternalVideo) {
                            TextButton(onClick = { currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.size }) {
                                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${playbackSpeeds[currentSpeedIndex]}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) PaddingValues(0.dp) else paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // VIDEO PLAYER VIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.height(230.dp))
                    .background(Color.Black)
            ) {
                if (isExternalVideo) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                loadUrl(fullVideoUrl)
                            }
                        }
                    )
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                setShowFastForwardButton(true)
                                setShowRewindButton(true)
                            }
                        }
                    )
                }
            }

            if (!isLandscape) {
                // LECTURE ACTION BAR (Marrow/PrepLadder Style: Bookmark, Slides, Mark Complete, Notes)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bookmark Action
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                            coroutineScope.launch { progressRepo.toggleBookmark(module.id) }
                        }) {
                            Icon(
                                imageVector = if (isBookmarked.contains(module.id)) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked.contains(module.id)) Color(0xFF00C6A0) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Bookmark", style = MaterialTheme.typography.labelSmall)
                        }

                        // Mark Complete Action
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                            coroutineScope.launch { progressRepo.markModuleCompleted(module.id) }
                        }) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                contentDescription = "Complete",
                                tint = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Complete", style = MaterialTheme.typography.labelSmall)
                        }

                        // Slides / Notes Shortcut
                        if (module.notesUrl != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                onOpenNotes(module.notesUrl)
                            }) {
                                Icon(Icons.Outlined.Description, contentDescription = "Notes", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("Class Notes", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Related QBank Shortcut
                        if (module.relatedQuizUrl != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                onOpenRelatedQuiz(module.relatedQuizUrl)
                            }) {
                                Icon(Icons.Default.Quiz, contentDescription = "QBank", tint = Color(0xFFFF9800))
                                Spacer(Modifier.height(4.dp))
                                Text("MCQs", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // TABS: Subtopics / Chapters vs Notes Info
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Subtopics", fontWeight = FontWeight.Bold) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Lecture Info", fontWeight = FontWeight.Bold) })
                }

                when (selectedTab) {
                    0 -> {
                        // CHAPTER TIMESTAMPS LIST
                        if (module.timestamps.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No subtopics timestamped for this video.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                items(module.timestamps) { timestamp ->
                                    val minutes = timestamp.seconds / 60
                                    val secs = timestamp.seconds % 60
                                    val formattedTime = String.format("%02d:%02d", minutes, secs)

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                if (!isExternalVideo) {
                                                    exoPlayer.seekTo(timestamp.seconds * 1000)
                                                    exoPlayer.play()
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(12.dp))
                                            Text(formattedTime, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(16.dp))
                                            Text(timestamp.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            Text("Title:", fontWeight = FontWeight.Bold)
                            Text(module.title, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(16.dp))
                            Text("Type:", fontWeight = FontWeight.Bold)
                            Text(module.type.uppercase(), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            Text("Playback Source:", fontWeight = FontWeight.Bold)
                            Text(if (isExternalVideo) "External Web Stream" else "Direct Video Stream", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
