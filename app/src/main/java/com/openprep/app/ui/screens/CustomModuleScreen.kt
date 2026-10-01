package com.openprep.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    var selectedQuestionCount by remember { mutableIntStateOf(25) }
    var selectedMode by remember { mutableStateOf("Practice") } // "Practice" (instant review) vs "Exam" (timed)
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Incorrect", "Unattempted", "Bookmarked"
    var selectedDifficulty by remember { mutableStateOf("All") }

    // Multi-select subjects
    val selectedSubjects = remember { mutableStateListOf<String>().apply { addAll(manifest.subjects.map { it.id }) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Granular Test Generator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sizing Presets
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Number of Questions", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 25, 50, 100).forEach { count ->
                            FilterChip(
                                selected = selectedQuestionCount == count,
                                onClick = { selectedQuestionCount = count },
                                label = { Text("$count Qs") }
                            )
                        }
                    }
                }
            }

            // Mode Selection (Marrow Practice vs Exam Mode)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Test Mode", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedMode == "Practice",
                            onClick = { selectedMode = "Practice" },
                            label = { Text("Practice (Instant Solutions)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == "Exam",
                            onClick = { selectedMode = "Exam" },
                            label = { Text("Exam (Timed Final)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Subject Granular Selection
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Subjects (${selectedSubjects.size}/${manifest.subjects.size})", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(manifest.subjects) { subject ->
                            val isSelected = selectedSubjects.contains(subject.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedSubjects.remove(subject.id)
                                    else selectedSubjects.add(subject.id)
                                },
                                label = { Text(subject.title) }
                            )
                        }
                    }
                }
            }

            // Question Pool Filters
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Question Targeting", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf("All Questions", "Unattempted Only", "Mistakes / Incorrect Only", "Bookmarked MCQs").forEach { filter ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedFilter == filter, onClick = { selectedFilter = filter })
                            Text(filter)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val matchingQuizModule = manifest.subjects
                        .filter { selectedSubjects.contains(it.id) }
                        .flatMap { it.modules }
                        .find { it.type.lowercase() == "qbank" }

                    if (matchingQuizModule != null) {
                        onLaunchCustomTest(matchingQuizModule.url)
                    } else {
                        Toast.makeText(context, "No MCQs found in the selected subjects.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Custom Module ($selectedQuestionCount Qs)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
