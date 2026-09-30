package com.openprep.app.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    val title: String,
    val type: String,
    val timestamp: Long
)
