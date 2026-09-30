package com.openprep.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    progressRepo: ProgressRepository,
    onNavigateToDownloads: () -> Unit,
    onSyncRequested: () -> Unit, // NEW: Sync trigger
    onDisconnect: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val userProfile by progressRepo.getUserProfile().collectAsState(initial = Pair("Learner", "General Prep"))
    var showEditProfile by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var examInput by remember { mutableStateOf("") }

    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Name") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = examInput, onValueChange = { examInput = it }, label = { Text("Target Exam") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch { progressRepo.saveUserProfile(nameInput, examInput) }
                    showEditProfile = false
                }) { Text("Save") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userProfile.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(userProfile.second, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { 
                        nameInput = userProfile.first
                        examInput = userProfile.second
                        showEditProfile = true 
                    }) { Text("EDIT") }
                }
            }

            Text("App Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToDownloads) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text("Manage Offline Downloads", fontWeight = FontWeight.Medium)
                }
            }

            // NEW: Sync Content Button
            Card(modifier = Modifier.fillMaxWidth(), onClick = {
                onSyncRequested()
                Toast.makeText(context, "Checking server for updates...", Toast.LENGTH_SHORT).show()
            }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Sync Latest Content", fontWeight = FontWeight.Medium)
                        Text("Check server for new modules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                    }
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Clear Progress", fontWeight = FontWeight.Bold)
                            Text("Reset scores & bookmarks", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            coroutineScope.launch {
                                progressRepo.clearAllProgress()
                                Toast.makeText(context, "Progress Cleared", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) { Text("Clear") }
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Disconnect from Server")
            }
        }
    }
}
