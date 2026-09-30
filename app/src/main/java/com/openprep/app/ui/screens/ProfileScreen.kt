package com.openprep.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.data.ProgressRepository
import kotlinx.coroutines.launch
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    progressRepo: ProgressRepository,
    onNavigateToDownloads: () -> Unit,
    onSyncRequested: () -> Unit,
    onDisconnect: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val userProfile by progressRepo.getUserProfile().collectAsState(initial = Pair("Learner", "General Prep"))
    var showEditProfile by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var examInput by remember { mutableStateOf("") }

    // EXPORT LAUNCHER
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val jsonString = progressRepo.exportProgress()
                    context.contentResolver.openFileDescriptor(it, "w")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos -> fos.write(jsonString.toByteArray()) }
                    }
                    Toast.makeText(context, "Progress Exported!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // IMPORT LAUNCHER
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val jsonString = inputStream?.bufferedReader().use { reader -> reader?.readText() } ?: ""
                    val success = progressRepo.importProgress(jsonString)
                    if (success) Toast.makeText(context, "Progress Restored!", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "Invalid Backup File", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    Scaffold(topBar = { TopAppBar(title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) }) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(userProfile.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(userProfile.second, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { nameInput = userProfile.first; examInput = userProfile.second; showEditProfile = true }) { Text("EDIT") }
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

            Card(modifier = Modifier.fillMaxWidth(), onClick = { onSyncRequested(); Toast.makeText(context, "Checking server for updates...", Toast.LENGTH_SHORT).show() }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Sync Latest Content", fontWeight = FontWeight.Medium)
                        Text("Check server for new modules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                    }
                }
            }

            Text("Data Ownership (BYOS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = { exportLauncher.launch("openprep_backup.json") }) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Export")
                }
                Button(modifier = Modifier.weight(1f), onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Import")
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Disconnect from Server")
            }
        }
    }
}
