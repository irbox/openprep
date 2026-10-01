package com.openprep.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.model.CourseManifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardHubScreen(
    manifest: CourseManifest,
    onOpenDeck: (String) -> Unit
) {
    val allDecks = manifest.subjects.flatMap { subject ->
        subject.modules.filter { it.type.lowercase() == "treasure" }.map { Pair(subject.title, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashcards & Spaced Repetition", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // High-yield Anki-style banner
                val bannerGradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.background(bannerGradient).padding(20.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("ACTIVE RECALL ENGINE", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Fast Visual Retention", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Native .apkg (Anki) and JSON decks with spaced repetition review ratings.", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Text("Available Decks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (allDecks.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Flashcard Decks Found", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Add .apkg (Anki) or JSON decks under type 'treasure' in your course manifest to study them here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(allDecks) { (subjectTitle, module) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDeck(module.url) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Style, contentDescription = null, tint = Color(0xFF8B5CF6))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(module.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(subjectTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            FilledIconButton(
                                onClick = { onOpenDeck(module.url) },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF8B5CF6))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start Deck", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
