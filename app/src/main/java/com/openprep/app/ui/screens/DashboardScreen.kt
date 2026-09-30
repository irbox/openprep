package com.openprep.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import com.openprep.app.model.Module
import com.openprep.app.utils.DownloadHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    currentServerUrl: String,
    onDisconnect: () -> Unit,
    onModuleClick: (Module) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(manifest.courseName) },
                actions = {
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Disconnect")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(manifest.subjects) { subject ->
                Text(
                    text = subject.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                
                subject.modules.forEach { module ->
                    ModuleCardItem(module, progressRepo, currentServerUrl) { onModuleClick(module) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCardItem(
    module: Module, 
    progressRepo: ProgressRepository, 
    currentServerUrl: String,
    onClick: () -> Unit
) {
    val isCompleted by progressRepo.isModuleCompleted(module.id).collectAsState(false)
    val score by progressRepo.getModuleScore(module.id).collectAsState(null)
    val bookmarkedModules by progressRepo.getBookmarkedModules().collectAsState(emptySet())
    val isBookmarked = bookmarkedModules.contains(module.id)
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon based on Type
            val icon = when(module.type.lowercase()) {
                "video" -> Icons.Default.PlayArrow
                "pdf" -> Icons.Default.Info
                else -> Icons.Default.Create
            }
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(text = module.title, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.type.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (module.type.lowercase() == "video" && isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Watched", tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                    } else if (module.type.lowercase() == "qbank" && score != null) {
                        Text("Best: $score", fontWeight = FontWeight.Bold, color = Color(0xFF00E676), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Actions (Bookmark & Download)
            Row {
                // Download Button (Only for PDFs and Videos)
                if (module.type.lowercase() in listOf("video", "pdf")) {
                    IconButton(onClick = {
                        val fullUrl = if (module.url.startsWith("http")) module.url else "$currentServerUrl/${module.url}"
                        val mime = if (module.type.lowercase() == "pdf") "application/pdf" else "video/mp4"
                        val ext = if (module.type.lowercase() == "pdf") ".pdf" else ".mp4"
                        val cleanName = module.title.replace(Regex("[^a-zA-Z0-9.-]"), "_") + ext
                        
                        DownloadHelper.downloadFile(context, fullUrl, cleanName, mime)
                    }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Download")
                    }
                }

                // Bookmark Button
                IconButton(onClick = {
                    coroutineScope.launch { progressRepo.toggleBookmark(module.id) }
                }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
