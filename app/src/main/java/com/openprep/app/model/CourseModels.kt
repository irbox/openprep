package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class CourseManifest(
    val courseName: String,
    val version: String,
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
    val type: String, // e.g., "video", "pdf", "qbank"
    val url: String   // The relative or absolute link to the file on your server
)
