package com.openprep.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // FIXED IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import com.openprep.app.model.Module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    currentServerUrl: String,
    onModuleClick: (Module) -> Unit
) {
    val bookmarkedIds by progressRepo.getBookmarkedModules().collectAsState(initial = emptySet())
    val bookmarkedModules = manifest.subjects.flatMap { it.modules }.filter { bookmarkedIds.contains(it.id) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Bookmarks") }) }
    ) { paddingValues ->
        if (bookmarkedModules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No bookmarks yet. Tap the heart on a module!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(bookmarkedModules) { module ->
                    ModuleCardItem(module = module, progressRepo = progressRepo, currentServerUrl = currentServerUrl, onClick = { onModuleClick(module) })
                }
            }
        }
    }
}
