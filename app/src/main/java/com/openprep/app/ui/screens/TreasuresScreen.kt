package com.openprep.app.ui.screens

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.Flashcard
import com.openprep.app.model.TreasureManifest
import com.openprep.app.utils.TTSHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuresScreen(
    url: String,
    progressRepo: ProgressRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ttsHelper = remember { TTSHelper(context) }
    DisposableEffect(Unit) { onDispose { ttsHelper.shutdown() } }

    var cards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                if (url.endsWith(".apkg", ignoreCase = true)) {
                    cards = loadAnkiApkg(context, url)
                } else {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: ""
                        val manifest = Json { ignoreUnknownKeys = true }.decodeFromString<TreasureManifest>(body)
                        cards = manifest.cards
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to load flashcard deck."
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spaced Repetition Deck") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (cards.isNotEmpty()) {
                        IconButton(onClick = {
                            val activeCard = cards[currentIndex]
                            ttsHelper.speak(if (isFlipped) activeCard.backText else activeCard.frontText)
                        }) {
                            Icon(Icons.Default.VolumeUp, "Pronounce")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else if (cards.isNotEmpty()) {
                val card = cards[currentIndex]
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(400),
                    label = "cardFlip"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Card ${currentIndex + 1} of ${cards.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (!card.tag.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                                Text("#${card.tag}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Flashcard with 3D Flip
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { isFlipped = !isFlipped }
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (rotation > 90f) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            if (rotation <= 90f) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(card.frontText, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Tap to reveal answer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            } else {
                                Text(
                                    card.backText,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Anki Evaluation Rating Buttons (Shown when card is revealed)
                    if (isFlipped) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch { progressRepo.recordCardReview(card.id.ifEmpty { "$currentIndex" }, "again") }
                                    isFlipped = false
                                    currentIndex = (currentIndex + 1) % cards.size
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Again")
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch { progressRepo.recordCardReview(card.id.ifEmpty { "$currentIndex" }, "hard") }
                                    isFlipped = false
                                    currentIndex = (currentIndex + 1) % cards.size
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Hard")
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch { progressRepo.recordCardReview(card.id.ifEmpty { "$currentIndex" }, "good") }
                                    isFlipped = false
                                    currentIndex = (currentIndex + 1) % cards.size
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Good")
                            }
                        }
                    } else {
                        Button(
                            onClick = { isFlipped = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reveal Answer")
                        }
                    }
                }
            } else {
                Text("No cards found in this deck.")
            }
        }
    }
}

private fun loadAnkiApkg(context: Context, url: String): List<Flashcard> {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    val response = client.newCall(request).execute()
    val inputStream = response.body?.byteStream() ?: return emptyList()

    val dbFile = File(context.cacheDir, "collection.anki2")
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
    if (dbFile.exists()) {
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = db.rawQuery("SELECT id, flds FROM notes", null)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0).toString()
            val flds = cursor.getString(1)
            val parts = flds.split("\u001F")
            if (parts.size >= 2) {
                val front = parts[0].replace(Regex("<.*?>"), "")
                val back = parts[1].replace(Regex("<.*?>"), "")
                extractedCards.add(Flashcard(id = id, frontText = front, backText = back, tag = "Anki"))
            }
        }
        cursor.close()
        db.close()
    }
    return extractedCards
}
