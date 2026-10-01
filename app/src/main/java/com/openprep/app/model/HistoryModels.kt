package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    val title: String,
    val type: String,
    val timestamp: Long
)

@Serializable
data class ProgressBackup(
    val scores: Map<String, Int> = emptyMap(),
    val completed: List<String> = emptyList(),
    val bookmarks: List<String> = emptyList(),
    val history: String = "[]",
    val streak: Int = 0,
    val lastOpened: Long = 0L,
    val themeMode: Int = 0
)
