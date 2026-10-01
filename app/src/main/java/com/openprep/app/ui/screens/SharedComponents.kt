package com.openprep.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openprep.app.R
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.Module
import com.openprep.app.utils.DownloadHelper
import kotlinx.coroutines.launch

val GradientStart = Color(0xFF00E676)
val GradientEnd = Color(0xFF1DE9B6)
val IconBgLight = Color(0xFFE0F2F1)
val SuccessGrn = Color(0xFF00C853)
val TextSecLight = Color(0xFF6B7280)

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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val iconContent = when (module.type.lowercase()) {
                "video" -> Icons.Default.PlayArrow
                "pdf" -> Icons.Default.Description
                "qbank" -> null
                "treasure" -> Icons.Default.Style
                "image" -> Icons.Default.Image
                else -> Icons.Default.Info
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (iconContent != null) {
                    Icon(imageVector = iconContent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                } else {
                    Icon(painter = painterResource(id = R.drawable.ic_subject_mcq), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = module.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = module.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecLight)
                    Spacer(Modifier.width(8.dp))
                    if (module.type.lowercase() == "video" && isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Watched", tint = SuccessGrn, modifier = Modifier.size(14.dp))
                    } else if (module.type.lowercase() == "qbank" && score != null) {
                        Text("Best: $score", fontWeight = FontWeight.Bold, color = SuccessGrn, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row {
                if (module.type.lowercase() in listOf("video", "pdf", "image")) {
                    IconButton(onClick = {
                        val fullUrl = if (module.url.startsWith("http")) module.url else "$currentServerUrl/${module.url}"
                        val mime = when (module.type.lowercase()) { "pdf" -> "application/pdf"; "image" -> "image/*"; else -> "video/mp4" }
                        val ext = when (module.type.lowercase()) { "pdf" -> ".pdf"; "image" -> ".jpg"; else -> ".mp4" }
                        DownloadHelper.downloadFile(context, fullUrl, module.title + ext, mime)
                    }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Download", tint = TextSecLight)
                    }
                }

                IconButton(onClick = { coroutineScope.launch { progressRepo.toggleBookmark(module.id) } }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color(0xFFE91E63) else TextSecLight
                    )
                }
            }
        }
    }
}
