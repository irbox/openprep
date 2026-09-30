package com.openprep.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository
) {
    val prefs by progressRepo.getAllPreferences().collectAsState(initial = null)

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Progress") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (prefs == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                return@Scaffold
            }

            var totalVideos = 0
            var watchedVideos = 0
            var totalQuizzes = 0
            var attemptedQuizzes = 0
            var totalScore = 0

            // Calculate progress instantly offline
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
            
            // Video Stats Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Video Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = videoProgress,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("$watchedVideos of $totalVideos Videos Watched")
                }
            }

            // Quiz Stats Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quiz Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("Quizzes Attempted: $attemptedQuizzes / $totalQuizzes")
                    if (attemptedQuizzes > 0) {
                        Text(
                            text = "Total Score Across All Quizzes: $totalScore",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
