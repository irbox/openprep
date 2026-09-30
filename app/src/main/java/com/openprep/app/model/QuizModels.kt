package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizManifest(
    val title: String,
    val durationMinutes: Int = 15, // NEW: Default to 15 mins if not provided in JSON
    val questions: List<Question>
)

@Serializable
data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String
)
