package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class QuizManifest(
    val title: String,
    val durationMinutes: Int = 15,
    val questions: List<Question>
)

@Serializable
data class Question(
    val id: String,
    val text: String,
    val imageUrl: String? = null,
    val audioExplanationUrl: String? = null,
    val difficulty: String = "Medium",
    val peerAccuracyPercent: Int = 50,
    val options: List<String>,
    val peerOptionPercentages: List<Int> = emptyList(),
    val correctOptionIndex: Int,
    val explanation: String
)
