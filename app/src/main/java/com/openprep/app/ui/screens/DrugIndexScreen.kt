package com.openprep.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.model.DrugItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugIndexScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Curated Official Government & Statutory Clinical Reference Databases
    val officialPortals = remember {
        listOf(
            DrugItem(
                name = "CDSCO Drug Information Portal",
                category = "Regulatory Authority (India)",
                officialUrl = "https://cdsco.gov.in/",
                description = "Central Drugs Standard Control Organisation official database of approved drugs, clinical trials, and safety alerts.",
                authority = "Ministry of Health & Family Welfare, Govt of India"
            ),
            DrugItem(
                name = "National List of Essential Medicines (NLEM)",
                category = "Essential Medicines",
                officialUrl = "https://cdsco.gov.in/opencms/opencms/en/Drugs/NLEM/",
                description = "Official guidance on therapeutic efficacy, cost-effectiveness, and priority healthcare medications.",
                authority = "Government of India"
            ),
            DrugItem(
                name = "National Formulary of India (NFI)",
                category = "Prescribing Guidance",
                officialUrl = "https://ipc.gov.in/national-formulary-of-india.html",
                description = "Authoritative guidance on rational drug use, dosing schedules, contraindications, and drug interactions.",
                authority = "Indian Pharmacopoeia Commission"
            ),
            DrugItem(
                name = "MedlinePlus Drug Information",
                category = "Clinical Reference (US / Global)",
                officialUrl = "https://medlineplus.gov/druginformation.html",
                description = "Comprehensive, peer-reviewed clinical monographs on thousands of prescription and OTC medications.",
                authority = "National Library of Medicine / NIH"
            ),
            DrugItem(
                name = "DailyMed Official Label Repository",
                category = "Pharmacological Package Inserts",
                officialUrl = "https://dailymed.nlm.nih.gov/dailymed/",
                description = "FDA-approved labeling, chemical structures, adverse reaction profiles, and dosage forms.",
                authority = "U.S. National Library of Medicine / FDA"
            ),
            DrugItem(
                name = "WHO Model List of Essential Medicines",
                category = "Global Formulary",
                officialUrl = "https://www.who.int/groups/expert-committee-on-selection-and-use-of-essential-medicines/essential-medicines-lists",
                description = "Global gold standard for medications needed in any basic healthcare system.",
                authority = "World Health Organization"
            )
        )
    }

    val filtered = if (searchQuery.isBlank()) {
        officialPortals
    } else {
        officialPortals.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.authority.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Official Drug & Regulatory Portals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search portals (e.g., CDSCO, NLEM, FDA, WHO)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.officialUrl)))
                                } catch (e: Exception) {
                                    // Handled safely
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.Policy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Open External",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.category,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = item.authority,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
