package com.openprep.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openprep.app.model.QuizManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class QuizState {
    object Loading : QuizState()
    data class Active(
        val manifest: QuizManifest,
        val currentQuestionIndex: Int,
        val selectedOption: Int?,
        val hasSubmittedAnswer: Boolean,
        val score: Int
    ) : QuizState()
    data class Finished(val score: Int, val total: Int) : QuizState()
    data class Error(val message: String) : QuizState()
}

class QuizViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<QuizState>(QuizState.Loading)
    val uiState: StateFlow<QuizState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun loadQuiz(url: String) {
        _uiState.value = QuizState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Failed to load quiz")
                    
                    val body = response.body?.string() ?: throw Exception("Empty body")
                    val quiz = json.decodeFromString<QuizManifest>(body)
                    
                    _uiState.value = QuizState.Active(
                        manifest = quiz,
                        currentQuestionIndex = 0,
                        selectedOption = null,
                        hasSubmittedAnswer = false,
                        score = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.value = QuizState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun selectOption(index: Int) {
        val state = _uiState.value
        if (state is QuizState.Active && !state.hasSubmittedAnswer) {
            _uiState.value = state.copy(selectedOption = index)
        }
    }

    fun submitAnswer() {
        val state = _uiState.value
        if (state is QuizState.Active && state.selectedOption != null) {
            val isCorrect = state.selectedOption == state.manifest.questions[state.currentQuestionIndex].correctOptionIndex
            val newScore = if (isCorrect) state.score + 1 else state.score
            _uiState.value = state.copy(hasSubmittedAnswer = true, score = newScore)
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state is QuizState.Active) {
            if (state.currentQuestionIndex + 1 < state.manifest.questions.size) {
                _uiState.value = state.copy(
                    currentQuestionIndex = state.currentQuestionIndex + 1,
                    selectedOption = null,
                    hasSubmittedAnswer = false
                )
            } else {
                _uiState.value = QuizState.Finished(state.score, state.manifest.questions.size)
            }
        }
    }
}
