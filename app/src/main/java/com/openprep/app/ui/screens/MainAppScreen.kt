package com.openprep.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import com.openprep.app.model.Module

@Composable
fun MainAppScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    currentServerUrl: String,
    onDisconnect: () -> Unit,
    onSyncRequested: () -> Unit,
    onSubjectClick: (String) -> Unit,
    onModuleClick: (Module) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.MenuBook, "Library") }, label = { Text("Library") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.BarChart, "Progress") }, label = { Text("Progress") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Person, "Profile") }, label = { Text("Profile") })
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardScreen(manifest, progressRepo, currentServerUrl, onSubjectClick, onModuleClick)
                1 -> {
                    val bookManifest = manifest.copy(subjects = manifest.subjects.map { subj -> subj.copy(modules = subj.modules.filter { it.type.lowercase() == "pdf" }) }.filter { it.modules.isNotEmpty() })
                    DashboardScreen(bookManifest, progressRepo, currentServerUrl, onSubjectClick, onModuleClick)
                }
                2 -> StatsScreen(manifest, progressRepo)
                3 -> ProfileScreen(progressRepo, manifest.supportUrl, onNavigateToDownloads = { onModuleClick(Module("", "", "downloads", "")) }, onSyncRequested, onDisconnect)
            }
        }
    }
}
