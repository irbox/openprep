package com.openprep.app.model

import kotlinx.serialization.Serializable

// For the "Ask Doubts" support link in the main manifest
@Serializable
data class CourseManifest(
    val courseName: String,
    val version: String,
    val supportUrl: String? = null, // e.g., "mailto:tutor@me.com" or "https://discord.gg/..."
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
    val type: String, // "video", "pdf", "qbank", "treasure" (New!)
    val url: String 
)

// NEW: Data model for the Treasures (Flashcards)
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
