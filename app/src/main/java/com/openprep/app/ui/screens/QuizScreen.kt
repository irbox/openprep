package com.openprep.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <-- ADDED MISSING IMPORT
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.openprep.app.utils.TTSHelper
import com.openprep.app.viewmodel.QuizState
import com.openprep.app.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuizScreen(quizUrl: String, onNavigateBack: () -> Unit, viewModel: QuizViewModel = viewModel()) {
    val context = LocalContext.current
    val ttsHelper = remember { TTSHelper(context) }
    DisposableEffect(Unit) { onDispose { ttsHelper.shutdown() } }

    LaunchedEffect(quizUrl) { viewModel.loadQuiz(quizUrl) }
    val uiState by viewModel.uiState.collectAsState()

    var eliminatedOptions by remember { mutableStateOf(setOf<Int>()) }
    var isGuessAnswer by remember { mutableStateOf(false) }
    var showQuestionPalette by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is QuizState.Active) {
                        val state = uiState as QuizState.Active
                        val minutes = state.timeRemainingSeconds / 60
                        val seconds = state.timeRemainingSeconds % 60
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, "Timer", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(String.format("%02d:%02d", minutes, seconds), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("QBank")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (uiState is QuizState.Active) {
                        val state = uiState as QuizState.Active

                        IconButton(onClick = { showQuestionPalette = true }) {
                            Icon(Icons.Default.GridView, contentDescription = "All Questions")
                        }

                        IconButton(onClick = {
                            ttsHelper.speak(state.manifest.questions[state.currentQuestionIndex].text)
                        }) {
                            Icon(Icons.Default.VolumeUp, "Read Aloud", tint = MaterialTheme.colorScheme.primary)
                        }

                        val isMarked = state.markedForReview.contains(state.currentQuestionIndex)
                        IconButton(onClick = { viewModel.toggleMarkForReview() }) {
                            Icon(
                                if (isMarked) Icons.Default.Flag else Icons.Outlined.Flag,
                                "Mark",
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
                        Spacer(Modifier.height(8.dp))
                        Text("Score: ${state.score} / ${state.total}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = onNavigateBack) { Text("Return to Dashboard") }
                    }
                }
                is QuizState.Active -> {
                    val question = state.manifest.questions[state.currentQuestionIndex]

                    LaunchedEffect(state.currentQuestionIndex) {
                        eliminatedOptions = emptySet()
                        isGuessAnswer = false
                        ttsHelper.stop()
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Question ${state.currentQuestionIndex + 1} of ${state.manifest.questions.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = question.difficulty.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (!question.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = question.imageUrl,
                                contentDescription = "Clinical Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Text(text = question.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tip: Long-press any option to cross it out.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))

                        question.options.forEachIndexed { index, optionText ->
                            val isSelected = state.selectedOption == index
                            val isCorrect = index == question.correctOptionIndex
                            val isEliminated = eliminatedOptions.contains(index)
                            val peerPercent = question.peerOptionPercentages.getOrNull(index)

                            val bgColors = if (state.hasSubmittedAnswer) {
                                if (isCorrect) Color(0xFFE8F5E9) else if (isSelected) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                            } else MaterialTheme.colorScheme.surface

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (!state.hasSubmittedAnswer && !isEliminated) viewModel.selectOption(index)
                                        },
                                        onLongClick = {
                                            if (!state.hasSubmittedAnswer) {
                                                eliminatedOptions = if (isEliminated) eliminatedOptions - index else eliminatedOptions + index
                                            }
                                        }
                                    ),
                                colors = CardDefaults.cardColors(containerColor = bgColors),
                                border = if (isSelected && !state.hasSubmittedAnswer) CardDefaults.outlinedCardBorder(true) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    RadioButton(selected = isSelected, onClick = null, enabled = !isEliminated)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = optionText,
                                        modifier = Modifier.weight(1f),
                                        textDecoration = if (isEliminated) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (state.hasSubmittedAnswer && peerPercent != null) {
                                        Text(
                                            text = "$peerPercent%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        if (!state.hasSubmittedAnswer) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Checkbox(checked = isGuessAnswer, onCheckedChange = { isGuessAnswer = it })
                                Text("Mark as Guess", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (state.hasSubmittedAnswer) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${question.peerAccuracyPercent}% of peers got this correct", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall)
                                        IconButton(onClick = { ttsHelper.speak(question.explanation) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.VolumeUp, "Read", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text("Explanation:", fontWeight = FontWeight.Bold)
                                    Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (state.hasSubmittedAnswer) viewModel.nextQuestion()
                                else viewModel.submitAnswer()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = state.selectedOption != null || state.hasSubmittedAnswer
                        ) {
                            Text(if (state.hasSubmittedAnswer) "Next Question" else "Submit Answer")
                        }
                    }

                    if (showQuestionPalette) {
                        ModalBottomSheet(onDismissRequest = { showQuestionPalette = false }) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("All Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))

                                LazyVerticalGrid(columns = GridCells.Fixed(5), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.manifest.questions.size) { idx ->
                                        val isCurrent = idx == state.currentQuestionIndex
                                        val isFlagged = state.markedForReview.contains(idx)

                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                                    else if (isFlagged) Color(0xFFFF9800)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable {
                                                    while (state.currentQuestionIndex < idx) { viewModel.nextQuestion() }
                                                    showQuestionPalette = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${idx + 1}",
                                                color = if (isCurrent || isFlagged) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
