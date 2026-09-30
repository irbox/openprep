package com.openprep.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.openprep.app.viewmodel.QuizState
import com.openprep.app.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuizScreen(quizUrl: String, onNavigateBack: () -> Unit, viewModel: QuizViewModel = viewModel()) {
    LaunchedEffect(quizUrl) { viewModel.loadQuiz(quizUrl) }
    val uiState by viewModel.uiState.collectAsState()

    // OPTION ELIMINATION STATE (Stores indexes of crossed-out options)
    var eliminatedOptions by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (uiState is QuizState.Active) {
                        val state = uiState as QuizState.Active
                        val minutes = state.timeRemainingSeconds / 60
                        val seconds = state.timeRemainingSeconds % 60
                        val isLowTime = state.timeRemainingSeconds <= 60

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = "Timer", tint = if (isLowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(8.dp))
                            Text(String.format("%02d:%02d", minutes, seconds), color = if (isLowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    } else { Text("QBank") }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (uiState is QuizState.Active) {
                        val state = uiState as QuizState.Active
                        val isMarked = state.markedForReview.contains(state.currentQuestionIndex)
                        IconButton(onClick = { viewModel.toggleMarkForReview() }) {
                            Icon(
                                imageVector = if (isMarked) Icons.Default.Flag else Icons.Outlined.Flag,
                                contentDescription = "Mark for Review",
                                tint = if (isMarked) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (val state = uiState) {
                is QuizState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is QuizState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is QuizState.Finished -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Exam Completed!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text("Your Score: ${state.score} / ${state.total}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = onNavigateBack) { Text("Return to Dashboard") }
                    }
                }
                is QuizState.Active -> {
                    val question = state.manifest.questions[state.currentQuestionIndex]
                    
                    // Reset eliminated options when question changes
                    LaunchedEffect(state.currentQuestionIndex) { eliminatedOptions = emptySet() }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Question ${state.currentQuestionIndex + 1} of ${state.manifest.questions.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            if (state.markedForReview.contains(state.currentQuestionIndex)) {
                                Text("Flagged for Review", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF9800))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        // CLINICAL IMAGE SUPPORT
                        if (!question.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = question.imageUrl,
                                contentDescription = "Clinical Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().height(200.dp).padding(bottom = 16.dp).clip(RoundedCornerShape(8.dp))
                            )
                        }

                        Text(text = question.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Text("Long-press an option to eliminate it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                        Spacer(Modifier.height(16.dp))

                        question.options.forEachIndexed { index, optionText ->
                            val isSelected = state.selectedOption == index
                            val isCorrect = index == question.correctOptionIndex
                            val isEliminated = eliminatedOptions.contains(index)
                            
                            val backgroundColor = if (state.hasSubmittedAnswer) {
                                if (isCorrect) Color(0xFFE8F5E9) else if (isSelected) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                            } else MaterialTheme.colorScheme.surface

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = { if (!state.hasSubmittedAnswer && !isEliminated) viewModel.selectOption(index) },
                                        onLongClick = { 
                                            if (!state.hasSubmittedAnswer) {
                                                eliminatedOptions = if (isEliminated) eliminatedOptions - index else eliminatedOptions + index
                                            }
                                        }
                                    ),
                                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                border = if (isSelected && !state.hasSubmittedAnswer) CardDefaults.outlinedCardBorder(true) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                    RadioButton(selected = isSelected, onClick = null, enabled = !isEliminated)
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = optionText, 
                                        color = if (isEliminated) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (isEliminated) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                        }

                        if (state.hasSubmittedAnswer) {
                            Spacer(Modifier.height(24.dp))
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Explanation:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(question.explanation)
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { if (state.hasSubmittedAnswer) viewModel.nextQuestion() else viewModel.submitAnswer() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = state.selectedOption != null || state.hasSubmittedAnswer
                        ) {
                            Text(if (state.hasSubmittedAnswer) "Next Question" else "Submit Answer")
                        }
                    }
                }
            }
        }
    }
}
