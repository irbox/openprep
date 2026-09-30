package com.openprep.app.ui.screens

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openprep.app.model.Flashcard
import com.openprep.app.model.TreasureManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TreasuresScreen(url: String, onNavigateBack: () -> Unit) {
    var cards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                if (url.endsWith(".apkg", ignoreCase = true)) {
                    // NATIVE ANKI INTEGRATION!
                    cards = loadAnkiApkg(context, url)
                } else {
                    // Fallback to our standard JSON
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: ""
                        val manifest = Json { ignoreUnknownKeys = true }.decodeFromString<TreasureManifest>(body)
                        cards = manifest.cards
                    }
                }
            } catch (e: Exception) { 
                errorMessage = e.localizedMessage ?: "Failed to load cards" 
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Treasures (Flashcards)") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage.isNotEmpty()) {
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            } else if (cards.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { cards.size })
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Swipe to view more. Tap to flip.", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(16.dp))
                    
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(400.dp)) { page ->
                        FlashcardItem(card = cards[page])
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Card ${pagerState.currentPage + 1} of ${cards.size}")
                }
            } else {
                Text("No flashcards found.")
            }
        }
    }
}

// ANKI EXTRACTOR LOGIC
private fun loadAnkiApkg(context: Context, url: String): List<Flashcard> {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    val inputStream = response.body?.byteStream() ?: return emptyList()
    
    val dbFile = File(context.cacheDir, "collection.anki2")
    
    // 1. Unzip the Anki file to find the SQLite database
    ZipInputStream(inputStream).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "collection.anki2") {
                dbFile.outputStream().use { out -> zis.copyTo(out) }
                break
            }
            entry = zis.nextEntry
        }
    }
    
    val extractedCards = mutableListOf<Flashcard>()
    
    // 2. Open the SQLite database natively using Android APIs
    if (dbFile.exists()) {
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = db.rawQuery("SELECT flds FROM notes", null)
        
        while (cursor.moveToNext()) {
            val flds = cursor.getString(0)
            // Anki separates the Front and Back of cards using a special character (\u001F)
            val parts = flds.split("\u001F") 
            if (parts.size >= 2) {
                // Strip HTML tags for clean display
                val front = parts[0].replace(Regex("<.*?>"), "")
                val back = parts[1].replace(Regex("<.*?>"), "")
                extractedCards.add(Flashcard(front, back))
            }
        }
        cursor.close()
        db.close()
    }
    return extractedCards
}

@Composable
fun FlashcardItem(card: Flashcard) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(500),
        label = "flip"
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
