package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.ui.viewmodels.AIBuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBuilderStudioScreen(
    viewModel: AIBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Builder Studio") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Input Section
            OutlinedTextField(
                value = uiState.userPrompt,
                onValueChange = { viewModel.onPromptChange(it) },
                label = { Text("Describe your project idea...") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.startOrchestration() },
                        enabled = !uiState.isProcessing && uiState.userPrompt.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                },
                enabled = !uiState.isProcessing
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress/Status Section
            if (uiState.isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.currentAgent?.displayName?.let { "$it is working..." } ?: "Orchestrating agents...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Section
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { AgentResultCard("Product Plan", uiState.report.productPlan, AgentType.PRODUCT_MANAGER) }
                item { AgentResultCard("Architecture", uiState.report.architecture, AgentType.ARCHITECT) }
                item { AgentResultCard("Generated Code", uiState.report.generatedCode, AgentType.DEVELOPER) }
                item { AgentResultCard("DevOps Strategy", uiState.report.devOpsStrategy, AgentType.DEVOPS) }
                item { AgentResultCard("Security Audit", uiState.report.securityAudit, AgentType.SECURITY) }
            }
        }
    }
}

@Composable
fun AgentResultCard(title: String, content: String, agentType: AgentType) {
    if (content.isBlank()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 20.sp
            )
        }
    }
}
