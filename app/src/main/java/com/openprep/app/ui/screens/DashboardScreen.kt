package com.openprep.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.R
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
    
    val lastPlayedId by progressRepo.getLastPlayedModuleId().collectAsState(initial = null)
    val lastPlayedModule = manifest.subjects.flatMap { it.modules }.find { it.id == lastPlayedId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    // 1:1 UI CLONE: Using the original App Logo in the Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_one_prep_logo), 
                            contentDescription = "Logo", 
                            modifier = Modifier.height(28.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(manifest.courseName, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // Hero Banner Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                // Background Graphic from original app
                Image(
                    painter = painterResource(id = R.drawable.ic_image_bg_prepare),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    alpha = 0.2f
                )
                
                Column(modifier = Modifier.padding(20.dp)) {
                    if (lastPlayedModule != null) {
                        Text("Resume Learning", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(lastPlayedModule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { onModuleClick(lastPlayedModule) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Continue")
                        }
                    } else {
                        Text("Ready to Learn?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("Select a subject below.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

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
                        shape = RoundedCornerShape(50)
                    )
                }
            }

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
            // Using precise icons
            val iconContent = when(module.type.lowercase()) {
                "video" -> Icons.Default.PlayArrow
                "pdf" -> Icons.Default.Menu 
                "qbank" -> null // We will use the custom painter below
                else -> Icons.Default.Info
            }
            
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (iconContent != null) {
                    Icon(imageVector = iconContent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    // 1:1 UI CLONE: Using the original App QBank Icon
                    Icon(painter = painterResource(id = R.drawable.ic_subject_mcq), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
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

            Row {
                if (module.type.lowercase() in listOf("video", "pdf")) {
                    IconButton(onClick = {
                        val fullUrl = if (module.url.startsWith("http")) module.url else "$currentServerUrl/${module.url}"
                        val mime = if (module.type.lowercase() == "pdf") "application/pdf" else "video/mp4"
                        val ext = if (module.type.lowercase() == "pdf") ".pdf" else ".mp4"
                        val cleanName = module.title.replace(Regex("[^a-zA-Z0-9.-]"), "_") + ext
                        com.openprep.app.utils.DownloadHelper.downloadFile(context, fullUrl, cleanName, mime)
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
