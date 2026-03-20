package com.ampgconsult.ibcn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.ui.viewmodels.GrowthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssetGeneratorScreen(
    onBack: () -> Unit,
    onViewMarketplace: () -> Unit,
    viewModel: GrowthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var ideaText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instant Asset Generator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!uiState.isGenerating && uiState.shareContent == null) {
                // Input Phase
                InputSection(
                    ideaText = ideaText,
                    onIdeaChange = { ideaText = it },
                    onGenerate = { viewModel.generateAndSell(ideaText) }
                )
            } else if (uiState.isGenerating) {
                // Generation Phase
                GenerationProgressSection(uiState.generationProgress)
            } else if (uiState.shareContent != null) {
                // Viral Growth Phase
                ViralGrowthSection(
                    shareContent = uiState.shareContent!!,
                    onViewMarketplace = onViewMarketplace
                )
            }
            
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun InputSection(ideaText: String, onIdeaChange: (String) -> Unit, onGenerate: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "What do you want to build and sell?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "AI will generate code, docs, and marketing assets instantly.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = ideaText,
            onValueChange = onIdeaChange,
            placeholder = { Text("e.g. A high-performance Flutter charting library") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 3
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = ideaText.isNotBlank()
        ) {
            Text("Generate & Sell Instantly", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GenerationProgressSection(progress: com.ampgconsult.ibcn.data.models.AgentResponse?) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            text = when (progress?.agentType) {
                AgentType.DEVELOPER -> "Generating Production-Ready Code..."
                AgentType.DOC_GENERATOR -> "Creating Documentation & SEO Assets..."
                AgentType.PRODUCT_MANAGER -> "Strategizing Pricing & Growth..."
                else -> "Initializing AI Creator Engine..."
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text(
                        text = progress?.content ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun ViralGrowthSection(shareContent: String, onViewMarketplace: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Green
        )
        Spacer(Modifier.height(16.dp))
        Text("Asset Published & Live!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Viral Growth Engine Activated 🔥", color = MaterialTheme.colorScheme.secondary)
        
        Spacer(Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Suggested Viral Posts", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(shareContent, style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {}, // Trigger system share
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share Everywhere")
            }
            
            OutlinedButton(
                onClick = onViewMarketplace,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("View in Market")
            }
        }
    }
}
