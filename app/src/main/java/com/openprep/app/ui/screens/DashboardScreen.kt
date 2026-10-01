package com.openprep.app.ui.screens

import android.content.Intent
import android.net.Uri
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    manifest: CourseManifest,
    progressRepo: ProgressRepository,
    currentServerUrl: String,
    onSearchClick: () -> Unit,
    onCustomModuleClick: () -> Unit,
    onSubjectClick: (String) -> Unit,
    onModuleClick: (Module) -> Unit
) {
    val context = LocalContext.current
    val lastPlayedId by progressRepo.getLastPlayedModuleId().collectAsState(initial = null)
    val lastPlayedModule = manifest.subjects.flatMap { it.modules }.find { it.id == lastPlayedId }
    val userProfile by progressRepo.getUserProfile().collectAsState(initial = Pair("Learner", ""))
    val completedIds by progressRepo.getCompletedModules().collectAsState(initial = emptySet())

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // TOP HEADER: Branding & User greeting (No streak badge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_one_prep_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Hello, ${userProfile.first} 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(manifest.courseName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            // HERO BANNER: Resume Learning
            if (lastPlayedModule != null) {
                item {
                    val bannerGradient = Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF1DE9B6)))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bannerGradient)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_image_bg_prepare),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                            alpha = 0.2f
                        )
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("RESUME LEARNING", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(lastPlayedModule.title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Button(
                                onClick = { onModuleClick(lastPlayedModule) },
                                modifier = Modifier.padding(top = 16.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF009688))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Continue", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // CLINICAL QUICK ACTIONS: Modular Test Generator & Official Drug Reference
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCustomModuleClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Custom Test", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("Modular Generator", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val url = manifest.officialDrugDirectoryUrl ?: "https://medlineplus.gov/druginformation.html"
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    // Fallback handled safely
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color(0xFF3B82F6))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Drug Portal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("Govt Reference", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }

            // SUBJECTS GRID WITH COMPLETION METERS
            item {
                Text(
                    "Subjects & Curriculum",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 1000.dp)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(manifest.subjects) { subject ->
                        val completedCount = subject.modules.count { completedIds.contains(it.id) }
                        val progressPercent = if (subject.modules.isNotEmpty()) completedCount.toFloat() / subject.modules.size else 0f

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clickable { onSubjectClick(subject.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(subject.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Column {
                                    LinearProgressIndicator(
                                        progress = progressPercent,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text("${(progressPercent * 100).toInt()}% Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
