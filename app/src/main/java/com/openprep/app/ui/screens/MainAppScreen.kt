package com.openprep.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import com.openprep.app.model.Module

@Composable
fun MainAppScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    currentServerUrl: String,
    onDisconnect: () -> Unit,
    onModuleClick: (Module) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Search, "Search") }, label = { Text("Search") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.MenuBook, "Books") }, label = { Text("Books") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Chat, "Doubts") }, label = { Text("Doubts") })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("Settings") })
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardScreen(manifest, progressRepo, currentServerUrl, onDisconnect, onModuleClick)
                1 -> SearchScreen(manifest, progressRepo, currentServerUrl, onModuleClick)
                2 -> {
                    // Quick Library Filter (Replicates activity_books.xml)
                    val bookManifest = manifest.copy(subjects = manifest.subjects.map { subj -> 
                        subj.copy(modules = subj.modules.filter { it.type.lowercase() == "pdf" }) 
                    }.filter { it.modules.isNotEmpty() })
                    DashboardScreen(bookManifest, progressRepo, currentServerUrl, onDisconnect, onModuleClick)
                }
                3 -> SupportScreen(supportUrl = manifest.supportUrl)
                4 -> SettingsScreen(progressRepo, onDisconnect)
            }
        }
    }
}
