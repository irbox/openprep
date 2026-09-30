package com.openprep.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.openprep.app.ui.theme.*
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
    val userProfile by progressRepo.getUserProfile().collectAsState(initial = Pair("Learner", ""))
    val currentStreak by progressRepo.getCurrentStreak().collectAsState(initial = 0) // <-- STREAK DATA

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(painter = painterResource(id = R.drawable.ic_one_prep_logo), contentDescription = "Logo", modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Hello, ${userProfile.first} 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(manifest.courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
                
                // GAMIFICATION: The Fire Streak Badge!
                Surface(
                    color = Color(0xFFFFF3E0), // Light Orange
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$currentStreak", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                val bannerGradient = Brush.linearGradient(listOf(GradientStart, GradientEnd))
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(bannerGradient)) {
                    Image(painter = painterResource(id = R.drawable.ic_image_bg_prepare), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(), alpha = 0.2f)
                    Column(modifier = Modifier.padding(24.dp)) {
                        if (lastPlayedModule != null) {
                            Text("RESUME LEARNING", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(lastPlayedModule.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            
                            Button(onClick = { onModuleClick(lastPlayedModule) }, modifier = Modifier.padding(top = 16.dp), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryTealDark)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Continue", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("READY TO LEARN?", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Select a subject below to begin.", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text("Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground)
                LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(manifest.subjects) { subject ->
                        val isSelected = selectedSubjectId == subject.id
                        Card(
                            modifier = Modifier.width(130.dp).height(85.dp).clickable { selectedSubjectId = subject.id },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 0.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
                                Text(subject.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            val selectedSubject = manifest.subjects.find { it.id == selectedSubjectId }
            if (selectedSubject != null) {
                items(selectedSubject.modules) { module ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        ModuleCardItem(module, progressRepo, currentServerUrl) { onModuleClick(module) }
                    }
                }
            }
        }
    }
}

// Keeping the exact same ModuleCardItem from the previous batch!
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleCardItem(module: Module, progressRepo: ProgressRepository, currentServerUrl: String, onClick: () -> Unit) {
    val isCompleted by progressRepo.isModuleCompleted(module.id).collectAsState(false)
    val score by progressRepo.getModuleScore(module.id).collectAsState(null)
    val bookmarkedModules by progressRepo.getBookmarkedModules().collectAsState(emptySet())
    val isBookmarked = bookmarkedModules.contains(module.id)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val iconContent = when(module.type.lowercase()) {
                "video" -> Icons.Default.PlayArrow
                "pdf" -> Icons.Default.Description 
                "qbank" -> null 
                "treasure" -> Icons.Default.Style
                "image" -> Icons.Default.Image
                else -> Icons.Default.Info
            }
            
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(IconBackgroundLight), contentAlignment = Alignment.Center) {
                if (iconContent != null) {
                    Icon(imageVector = iconContent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                } else {
                    Icon(painter = painterResource(id = R.drawable.ic_subject_mcq), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = module.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = module.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                    Spacer(Modifier.width(8.dp))
                    if (module.type.lowercase() == "video" && isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Watched", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    } else if (module.type.lowercase() == "qbank" && score != null) {
                        Text("Best: $score", fontWeight = FontWeight.Bold, color = SuccessGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row {
                if (module.type.lowercase() in listOf("video", "pdf", "image")) {
                    IconButton(onClick = {
                        val fullUrl = if (module.url.startsWith("http")) module.url else "$currentServerUrl/${module.url}"
                        val mime = when(module.type.lowercase()) { "pdf" -> "application/pdf"; "image" -> "image/*"; else -> "video/mp4" }
                        val ext = when(module.type.lowercase()) { "pdf" -> ".pdf"; "image" -> ".jpg"; else -> ".mp4" }
                        val cleanName = module.title.replace(Regex("[^a-zA-Z0-9.-]"), "_") + ext
                        DownloadHelper.downloadFile(context, fullUrl, cleanName, mime)
                    }) { Icon(Icons.Default.CloudDownload, contentDescription = "Download", tint = TextSecondaryLight) }
                }

                IconButton(onClick = { coroutineScope.launch { progressRepo.toggleBookmark(module.id) } }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFE91E63) else TextSecondaryLight
                    )
                }
            }
        }
    }
}
