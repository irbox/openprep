package com.openprep.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.Module
import com.openprep.app.model.Subject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectScreen(
    subject: Subject,
    progressRepo: ProgressRepository,
    currentServerUrl: String,
    onNavigateBack: () -> Unit,
    onModuleClick: (Module) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Videos", "Notes", "QBank", "Flashcards")

    val filteredModules = if (selectedFilter == "All") {
        subject.modules
    } else {
        val typeFilter = when (selectedFilter) {
            "Videos" -> "video"
            "Notes" -> "pdf"
            "QBank" -> "qbank"
            "Flashcards" -> "treasure"
            else -> ""
        }
        subject.modules.filter { it.type.lowercase() == typeFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject.title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            if (filteredModules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No modules found for this filter.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredModules) { module ->
                        ModuleCardItem(module, progressRepo, currentServerUrl) { onModuleClick(module) }
                    }
                }
            }
        }
    }
}
