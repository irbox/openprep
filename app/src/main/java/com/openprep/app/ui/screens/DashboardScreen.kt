package com.openprep.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var selectedSubjectId by remember { mutableStateOf(manifest.subjects.firstOrNull()?.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hero Section (Replicating original UI welcome banner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Welcome to ${manifest.courseName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Select a subject below to start learning.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Horizontal Subject Selector (Chips)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(manifest.subjects) { subject ->
                    val isSelected = selectedSubjectId == subject.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSubjectId = subject.id },
                        label = { Text(subject.title, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(50) // Pill shape like original
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Vertical Module List
            val selectedSubject = manifest.subjects.find { it.id == selectedSubjectId }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedSubject != null) {
                    items(selectedSubject.modules) { module ->
                        ModuleCardItem(module, progressRepo, currentServerUrl) { onModuleClick(module) }
                    }
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

    // ElevatedCard provides the beautiful shadow seen in the original app
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FOSS Material Icons replacing proprietary drawables
            val icon = when(module.type.lowercase()) {
                "video" -> Icons.Default.PlayArrow
                "pdf" -> Icons.Default.Menu // Represents document/notes
                "qbank" -> Icons.Default.Create
                else -> Icons.Default.Info
            }
            
            // Icon container with colored background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = module.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.type.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (module.type.lowercase() == "video" && isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Watched", tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                    } else if (module.type.lowercase() == "qbank" && score != null) {
                        Text("Best: $score", fontWeight = FontWeight.Bold, color = Color(0xFF00C853), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // FOSS Material Action Icons
            Row {
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

                IconButton(onClick = { coroutineScope.launch { progressRepo.toggleBookmark(module.id) } }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
