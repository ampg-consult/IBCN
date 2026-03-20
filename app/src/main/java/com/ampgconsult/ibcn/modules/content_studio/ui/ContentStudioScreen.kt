package com.ampgconsult.ibcn.modules.content_studio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampgconsult.ibcn.modules.content_studio.models.ContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentStudioScreen(
    onBack: () -> Unit,
    onGenerate: (String, ContentType) -> Unit,
    isGenerating: Boolean,
    generatedOutput: String?
) {
    var prompt by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ContentType.ARTICLE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Content Studio") },
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
        ) {
            Text("Create Professional Content", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name.replace("_", " ")) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Topic or Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onGenerate(prompt, selectedType) },
                modifier = Modifier.fillMaxWidth(),
                enabled = prompt.isNotBlank() && !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Content")
                }
            }
            
            if (generatedOutput != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Generated Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = generatedOutput,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
