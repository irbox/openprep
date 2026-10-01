package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class CourseManifest(
    val courseName: String,
    val version: String,
    val supportUrl: String? = null,
    val drugIndexUrl: String? = null, // Link to offline Clinical/Drug Index JSON
    val subjects: List<Subject>
)

@Serializable
data class Subject(
    val id: String,
    val title: String,
    val modules: List<Module>
)

@Serializable
data class Module(
    val id: String,
    val title: String,
    val type: String, // "video", "pdf", "qbank", "treasure", "image", "article"
    val url: String,
    val durationMinutes: Int? = null,
    val notesUrl: String? = null, // Attached PDF notes
    val relatedQuizUrl: String? = null, // Attached QBank
    val timestamps: List<VideoTimestamp> = emptyList() // Subtopic chapter markers
)

@Serializable
data class VideoTimestamp(
    val title: String,
    val seconds: Long
)

// QBank Model with Peer Stats & Audio/TTS
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
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val peerAccuracyPercent: Int = 50, // e.g. 58% got this correct
    val options: List<String>,
    val peerOptionPercentages: List<Int> = emptyList(), // e.g. [15, 18, 58, 9]
    val correctOptionIndex: Int,
    val explanation: String
)

// Flashcards & Clinical Index Models
@Serializable
data class TreasureManifest(
    val title: String,
    val cards: List<Flashcard>
)

@Serializable
data class Flashcard(
    val frontText: String,
    val backText: String
)

@Serializable
data class DrugItem(
    val name: String,
    val category: String,
    val brandNames: List<String> = emptyList(),
    val description: String = ""
)
