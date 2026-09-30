package com.openprep.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository
) {
    val prefs by progressRepo.getAllPreferences().collectAsState(initial = null)
    val history by progressRepo.getHistory().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Progress", fontWeight = FontWeight.Bold) }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (prefs == null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    return@item
                }

                var totalVideos = 0
                var watchedVideos = 0
                var totalQuizzes = 0
                var attemptedQuizzes = 0
                var totalScore = 0

                manifest.subjects.forEach { subject ->
                    subject.modules.forEach { module ->
                        if (module.type.lowercase() == "video") {
                            totalVideos++
                            val isWatched = prefs!![booleanPreferencesKey("completed_${module.id}")] ?: false
                            if (isWatched) watchedVideos++
                        } else if (module.type.lowercase() == "qbank") {
                            totalQuizzes++
                            val score = prefs!![intPreferencesKey("score_${module.id}")]
                            if (score != null) {
                                attemptedQuizzes++
                                totalScore += score
                            }
                        }
                    }
                }

                val videoProgress = if (totalVideos > 0) watchedVideos.toFloat() / totalVideos else 0f
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Videos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = videoProgress, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("$watchedVideos / $totalVideos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Quizzes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = if(totalQuizzes>0) attemptedQuizzes.toFloat()/totalQuizzes else 0f, modifier = Modifier.fillMaxWidth().height(6.dp), color = Color(0xFFFF9800))
                            Spacer(Modifier.height(8.dp))
                            Text("Score: $totalScore", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Text("Learning Timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                if (history.isEmpty()) {
                    Text("No activity yet. Start learning!", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                }
            }

            itemsIndexed(history) { index, item ->
                val isLast = index == history.size - 1
                val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                val dateStr = dateFormat.format(Date(item.timestamp))

                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.TopCenter) {
                        if (!isLast) {
                            val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            Canvas(modifier = Modifier.fillMaxHeight().width(2.dp).padding(top = 24.dp)) {
                                drawLine(color = lineColor, start = Offset(size.width/2, 0f), end = Offset(size.width/2, size.height), strokeWidth = 4f)
                            }
                        }
                        Box(modifier = Modifier.padding(top = 16.dp).size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }

                    Card(
                        modifier = Modifier.weight(1f).padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val iconContent = when(item.type.lowercase()) {
                                "video" -> Icons.Default.PlayArrow
                                "pdf" -> Icons.Default.Description 
                                "qbank" -> Icons.Default.Create
                                else -> Icons.Default.Info
                            }
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(imageVector = iconContent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
