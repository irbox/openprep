package com.openprep.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModuleScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    onNavigateBack: () -> Unit,
    onLaunchCustomTest: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedQuestionCount by remember { mutableIntStateOf(20) }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Incorrect", "Unattempted", "Bookmarked"
    var selectedDifficulty by remember { mutableFloatStateOf(1f) } // 0=Easy, 1=Medium, 2=Hard

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Module Generator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Create Your Own Test in Minutes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Number of Questions
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Number of Questions: $selectedQuestionCount", fontWeight = FontWeight.Bold)
                    Slider(
                        value = selectedQuestionCount.toFloat(),
                        onValueChange = { selectedQuestionCount = it.toInt() },
                        valueRange = 5f..50f,
                        steps = 8
                    )
                }
            }

            // Question Pool Mode
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Question Pool", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf("All Questions", "Incorrect Questions Only", "Unattempted Questions Only", "Bookmarked MCQs").forEach { filter ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedFilter == filter, onClick = { selectedFilter = filter })
                            Text(filter, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Difficulty Slider
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val diffLabel = when (selectedDifficulty.toInt()) {
                        0 -> "Easy"
                        2 -> "Hard"
                        else -> "Medium"
                    }
                    Text("Target Difficulty: $diffLabel", fontWeight = FontWeight.Bold)
                    Slider(
                        value = selectedDifficulty,
                        onValueChange = { selectedDifficulty = it },
                        valueRange = 0f..2f,
                        steps = 1
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    // Grab any available QBank module in the course to test with
                    val anyQuizModule = manifest.subjects.flatMap { it.modules }.find { it.type.lowercase() == "qbank" }
                    if (anyQuizModule != null) {
                        onLaunchCustomTest(anyQuizModule.url)
                    } else {
                        Toast.makeText(context, "No QBank modules found in this course to build from.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate & Start Test", fontWeight = FontWeight.Bold)
            }
        }
    }
}
