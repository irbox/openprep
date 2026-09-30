package com.openprep.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openprep.app.R
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import com.openprep.app.model.Module

val GradientStart = Color(0xFF00E676)
val GradientEnd = Color(0xFF1DE9B6)
val IconBgLight = Color(0xFFE0F2F1)
val SuccessGrn = Color(0xFF00C853)
val TextSecLight = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    onSubjectClick: (String) -> Unit,
    onModuleClick: (Module) -> Unit
) {
    val lastPlayedId by progressRepo.getLastPlayedModuleId().collectAsState(initial = null)
    val lastPlayedModule = manifest.subjects.flatMap { it.modules }.find { it.id == lastPlayedId }
    val userProfile by progressRepo.getUserProfile().collectAsState(initial = Pair("Learner", ""))
    val currentStreak by progressRepo.getCurrentStreak().collectAsState(initial = 0) 
    
    var searchQuery by remember { mutableStateOf("") }
    val allModules = manifest.subjects.flatMap { it.modules }
    val searchResults = if (searchQuery.isBlank()) emptyList() else allModules.filter { it.title.contains(searchQuery, true) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // TOP HEADER
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
                    Text("Hello, ${userProfile.first} 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(manifest.courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(50)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$currentStreak", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
            }
        }

        // GLOBAL SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("Search videos, notes, quizzes...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        if (searchQuery.isNotBlank()) {
            // SHOW SEARCH RESULTS
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(searchResults) { module -> ModuleCardItem(module, progressRepo, "") { onModuleClick(module) } }
            }
        } else {
            // SHOW NORMAL DASHBOARD
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                
                // HERO BANNER: Resume Learning
                if (lastPlayedModule != null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))) {
                            Image(painter = painterResource(id = R.drawable.ic_image_bg_prepare), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(), alpha = 0.2f)
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("RESUME LEARNING", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))
                                Text(lastPlayedModule.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Button(onClick = { onModuleClick(lastPlayedModule) }, modifier = Modifier.padding(top = 16.dp), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF009688))) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Continue", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // SUBJECT GRID (Marrow/PW Style)
                item {
                    Text("Your Subjects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                }
                
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 1000.dp).padding(horizontal = 20.dp), // constraint height
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false // Let LazyColumn handle scroll
                    ) {
                        items(manifest.subjects) { subject ->
                            Card(
                                modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onSubjectClick(subject.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    Text(subject.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
