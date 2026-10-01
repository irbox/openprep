package com.openprep.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openprep.app.model.DrugItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugIndexScreen(onNavigateBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    // Built-in high-yield clinical pharmacological database
    val sampleDrugs = remember {
        listOf(
            DrugItem("Acarbose", "Anti-Diabetic", listOf("Glucobay", "Asucrose"), "Alpha-glucosidase inhibitor that delays carbohydrate absorption."),
            DrugItem("Fulvestrant", "Anti-Neoplastic", listOf("Faslodex"), "Estrogen receptor antagonist used in hormone-receptor positive metastatic breast cancer."),
            DrugItem("Gefitinib", "Targeted Therapy", listOf("Iressa", "Geftinat"), "EGFR tyrosine kinase inhibitor for non-small cell lung cancer."),
            DrugItem("Glibenclamide", "Anti-Diabetic", listOf("Daonil", "Euglucon"), "Second-generation sulfonylurea that stimulates beta-cell insulin secretion."),
            DrugItem("Glimepiride", "Anti-Diabetic", listOf("Amaryl", "Gepride"), "Long-acting sulfonylurea with lower risk of hypoglycemia."),
            DrugItem("Hydrochlorothiazide", "Diuretic / Anti-Hypertensive", listOf("Aquazide"), "Thiazide diuretic inhibiting NaCl cotransport in distal convoluted tubule."),
            DrugItem("Hydroxyurea", "Anti-Metabolite", listOf("Hydrea", "Droxia"), "Inhibits ribonucleotide reductase; increases fetal hemoglobin in sickle cell."),
            DrugItem("Imatinib", "Tyrosine Kinase Inhibitor", listOf("Gleevec"), "First-line BCR-ABL tyrosine kinase inhibitor for Chronic Myeloid Leukemia (CML).")
        )
    }

    val filtered = if (searchQuery.isBlank()) sampleDrugs else sampleDrugs.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Drug Index", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search molecule or category (e.g. Diabetic)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { drug ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(drug.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(drug.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(drug.description, style = MaterialTheme.typography.bodySmall)
                            if (drug.brandNames.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Popular Brands: ${drug.brandNames.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}
