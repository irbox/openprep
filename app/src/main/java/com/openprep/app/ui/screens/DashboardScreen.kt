package com.openprep.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(id = R.drawable.ic_one_prep_logo), contentDescription = "Logo", modifier = Modifier.size(32.dp), tint = Color.Unspecified)
                        Spacer(Modifier.width(12.dp))
                        Text(manifest.courseName, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onDisconnect) { Icon(Icons.Default.ExitToApp, contentDescription = "Log Out") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1:1 HERO BANNER
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_image_bg_prepare), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(), alpha = 0.3f)
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (lastPlayedModule != null) {
                            Text("Resume Learning", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(lastPlayedModule.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Button(onClick = { onModuleClick(lastPlayedModule) }, modifier = Modifier.padding(top = 12.dp), shape = RoundedCornerShape(8.dp)) {
                                Text("Continue")
                            }
                        } else {
                            Text("Ready to Learn?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text("Select a subject below to begin.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // 1:1 SUBJECT CARDS
            item {
                Text("Subjects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(manifest.subjects) { subject ->
                        val isSelected = selectedSubjectId == subject.id
                        Card(
                            modifier = Modifier.width(140.dp).height(80.dp).clickable { selectedSubjectId = subject.id },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
                                Text(
                                    text = subject.title, 
                                    style = MaterialTheme.typography.labelLarge, 
                                    fontWeight = FontWeight.Bold, 
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 1:1 MODULE LIST
            val selectedSubject = manifest.subjects.find { it.id == selectedSubjectId }
            if (selectedSubject != null) {
                items(selectedSubject.modules) { module ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ModuleCardItem(module, progressRepo, currentServerUrl) { onModuleClick(module) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCardItem(module: Module, progressRepo: ProgressRepository, currentServerUrl: String, onClick: () -> Unit) {
    val isCompleted by progressRepo.isModuleCompleted(module.id).collectAsState(false)
    val score by progressRepo.getModuleScore(module.id).collectAsState(null)
    val bookmarkedModules by progressRepo.getBookmarkedModules().collectAsState(emptySet())
    val isBookmarked = bookmarkedModules.contains(module.id)
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val iconContent = when(module.type.lowercase()) {
                "video" -> Icons.Default.PlayArrow
                "pdf" -> Icons.Default.Description 
                "qbank" -> null 
                "treasure" -> Icons.Default.Style
                else -> Icons.Default.Info
            }
            
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                if (iconContent != null) {
                    Icon(imageVector = iconContent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(painter = painterResource(id = R.drawable.ic_subject_mcq), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = module.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = module.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.width(8.dp))
                    if (module.type.lowercase() == "video" && isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Watched", tint = Color(0xFF00C853), modifier = Modifier.size(14.dp))
                    } else if (module.type.lowercase() == "qbank" && score != null) {
                        Text("Best: $score", fontWeight = FontWeight.Bold, color = Color(0xFF00C853), style = MaterialTheme.typography.labelSmall)
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
                    }) { Icon(Icons.Default.CloudDownload, contentDescription = "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
