package com.openprep.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import com.openprep.app.model.Module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    currentServerUrl: String,
    onNavigateBack: () -> Unit,
    onModuleClick: (Module) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    val allModules = manifest.subjects.flatMap { it.modules }
    val filteredModules = if (searchQuery.isBlank()) emptyList() else allModules.filter { it.title.contains(searchQuery, true) }

    Scaffold { paddingValues ->
        SearchBar(
            modifier = Modifier.fillMaxWidth().padding(paddingValues),
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { active = false },
            active = active,
            onActiveChange = {
                active = it
                if (!active && searchQuery.isBlank()) {
                    onNavigateBack()
                }
            },
            placeholder = { Text("Search videos, notes, quizzes...") },
            leadingIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(filteredModules) { module ->
                    ModuleCardItem(module = module, progressRepo = progressRepo, currentServerUrl = currentServerUrl, onClick = { onModuleClick(module) })
                }
            }
        }
    }
}
