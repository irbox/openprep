package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class TreasureManifest(
    val title: String,
    val tag: String? = "General",
    val cards: List<Flashcard>
)

@Serializable
data class Flashcard(
    val id: String = "",
    val frontText: String,
    val backText: String,
    val tag: String? = null,
    val imageUrl: String? = null
)
