package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizManifest(
    val title: String,
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
