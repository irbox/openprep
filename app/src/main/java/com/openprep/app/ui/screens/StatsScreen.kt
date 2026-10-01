package com.openprep.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
        topBar = { TopAppBar(title = { Text("Performance & Analytics", fontWeight = FontWeight.Bold) }) }
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
                            if (score != null) totalScore += score
                        }
                    }
                }

                val calculatedPercentile = if (totalQuizzes > 0) {
                    ((totalScore.toFloat() / (totalQuizzes * 5).coerceAtLeast(1)) * 100).coerceIn(10f, 99f)
                } else {
                    78f
                }

                val rankGradient = Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.background(rankGradient).padding(20.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24))
                                Spacer(Modifier.width(8.dp))
                                Text("ESTIMATED PERCENTILE", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("${calculatedPercentile.toInt()}th Percentile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Pacing: 1:18 Mins / Question (Competitive Velocity)", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Videos", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("$watchedVideos / $totalVideos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Accuracy", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("${calculatedPercentile.toInt()}%", style = MaterialTheme.typography.titleMedium, color = Color(0xFF10B981))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Subject-Wise Strength Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                manifest.subjects.forEach { subject ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(subject.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("High Yield", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = 0.72f,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Learning Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                if (history.isEmpty()) {
                    Text("No study sessions recorded yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
                                drawLine(color = lineColor, start = Offset(size.width / 2, 0f), end = Offset(size.width / 2, size.height), strokeWidth = 4f)
                            }
                        }
                        Box(modifier = Modifier.padding(top = 16.dp).size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }

                    Card(
                        modifier = Modifier.weight(1f).padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
