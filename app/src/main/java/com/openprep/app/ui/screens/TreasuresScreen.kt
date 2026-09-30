package com.openprep.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TreasuresScreen(url: String, onNavigateBack: () -> Unit) {
    var manifest by remember { mutableStateOf<com.openprep.app.model.TreasureManifest?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    manifest = Json { ignoreUnknownKeys = true }.decodeFromString(body)
                }
            } catch (e: Exception) { e.printStackTrace() }
            isLoading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Treasures (Flashcards)") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (manifest != null) {
                val pagerState = rememberPagerState(pageCount = { manifest!!.cards.size })
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Swipe to view more. Tap to flip.", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(16.dp))
                    
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(400.dp)) { page ->
                        FlashcardItem(card = manifest!!.cards[page])
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Card ${pagerState.currentPage + 1} of ${manifest!!.cards.size}")
                }
            } else {
                Text("Failed to load treasures.")
            }
        }
    }
}

@Composable
fun FlashcardItem(card: com.openprep.app.model.Flashcard) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(500)
    )

    Card(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxSize()
            .clickable { flipped = !flipped }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (rotation > 90f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (rotation <= 90f) {
                Text(text = card.frontText, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            } else {
                Text(text = card.backText, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Color.White, modifier = Modifier.padding(16.dp).graphicsLayer { rotationY = 180f })
            }
        }
    }
}
