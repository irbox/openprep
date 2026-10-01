package com.openprep.app.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(videoUrl: String, title: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val playbackSpeeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
    var currentSpeedIndex by remember { mutableIntStateOf(0) }

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
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(currentSpeedIndex) { exoPlayer.playbackParameters = PlaybackParameters(playbackSpeeds[currentSpeedIndex]) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
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
                    title = { Text(text = title, maxLines = 1) },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                    actions = {
                        TextButton(onClick = { currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.size }) {
                            Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${playbackSpeeds[currentSpeedIndex]}x", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(if (isLandscape) PaddingValues(0.dp) else paddingValues).background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        
                        // PRO FEATURE: 10 second fast forward/rewind increments
                        setShowFastForwardButton(true)
                        setShowRewindButton(true)
                    }
                }
            )
            
            if (isLandscape) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50)
                ) {
                    TextButton(onClick = { currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.size }) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${playbackSpeeds[currentSpeedIndex]}x", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
