package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.AssetCategory
import com.ampgconsult.ibcn.ui.viewmodels.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishAssetScreen(
    onBack: () -> Unit,
    viewModel: MarketplaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var techStack by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AssetCategory.FLUTTER_UI) }
    var price by remember { mutableStateOf("0.0") }
    var assetUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publish Builder Asset") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Asset Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = techStack,
                onValueChange = { techStack = it },
                label = { Text("Tech Stack (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Flutter, Firebase, Dagger Hilt") }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.getAiSuggestions(title, techStack) },
                    enabled = title.isNotBlank() && techStack.isNotBlank() && !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Optimize Content")
                }
            }

            if (uiState.aiSuggestion != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Suggestions", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Suggested Title: ${uiState.aiSuggestion!!["title"]}", style = MaterialTheme.typography.bodySmall)
                        Text("Suggested Price: ${uiState.aiSuggestion!!["price"]}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.aiSuggestion!!["description"] ?: "", style = MaterialTheme.typography.bodySmall)
                        
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { 
                                title = uiState.aiSuggestion!!["title"] ?: title
                                price = uiState.aiSuggestion!!["price"] ?: price
                                description = uiState.aiSuggestion!!["description"] ?: description
                            }) {
                                Text("Apply All")
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = assetUrl,
                onValueChange = { assetUrl = it },
                label = { Text("Asset / Repo URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            AssetCategory.entries.forEach { cat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = category == cat, onClick = { category = cat })
                    Text(cat.displayName)
                }
            }

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price ($)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.publishAsset(
                        title = title,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        category = category.displayName,
                        techStack = techStack.split(",").map { it.trim() },
                        assetUrl = assetUrl
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank() && assetUrl.isNotBlank() && !uiState.isPublishing
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Publish Asset")
                }
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
