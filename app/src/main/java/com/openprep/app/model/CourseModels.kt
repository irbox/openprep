package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class CourseManifest(
    val courseName: String,
    val version: String,
    val supportUrl: String? = null,
    val drugIndexUrl: String? = null,
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
    val notesUrl: String? = null,
    val relatedQuizUrl: String? = null,
    val timestamps: List<VideoTimestamp> = emptyList()
)

@Serializable
data class VideoTimestamp(
    val title: String,
    val seconds: Long
)

@Serializable
data class DrugItem(
    val name: String,
    val category: String,
    val brandNames: List<String> = emptyList(),
    val description: String = ""
)
