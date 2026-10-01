package com.openprep.app.model

import kotlinx.serialization.Serializable

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
