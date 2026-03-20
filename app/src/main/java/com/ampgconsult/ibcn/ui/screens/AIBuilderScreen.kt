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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.ai.AIState
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.ui.viewmodels.AIBuilderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBuilderScreen(
    onBack: () -> Unit,
    viewModel: AIBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("AI Builder Studio", fontWeight = FontWeight.Bold)
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            // Input Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.userPrompt,
                        onValueChange = { viewModel.onPromptChange(it) },
                        label = { Text("What do you want to build?") },
                        placeholder = { Text("e.g. A social platform for builders...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isProcessing,
                        trailingIcon = {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = { 
                                        viewModel.startOrchestration() 
                                    },
                                    enabled = uiState.userPrompt.isNotBlank()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Agent Activity Panel
            if (uiState.isProcessing || uiState.report.productPlan.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item { 
                        AgentSection(
                            title = "Product Plan", 
                            content = uiState.report.productPlan, 
                            agent = AgentType.PRODUCT_MANAGER,
                            isActive = uiState.currentAgent == AgentType.PRODUCT_MANAGER
                        ) 
                    }
                    item { 
                        AgentSection(
                            title = "Architecture", 
                            content = uiState.report.architecture, 
                            agent = AgentType.ARCHITECT,
                            isActive = uiState.currentAgent == AgentType.ARCHITECT
                        ) 
                    }
                    item { 
                        AgentSection(
                            title = "Codebase", 
                            content = uiState.report.generatedCode, 
                            agent = AgentType.DEVELOPER,
                            isActive = uiState.currentAgent == AgentType.DEVELOPER,
                            isCode = true
                        ) 
                    }
                    item { 
                        AgentSection(
                            title = "DevOps", 
                            content = uiState.report.devOpsStrategy, 
                            agent = AgentType.DEVOPS,
                            isActive = uiState.currentAgent == AgentType.DEVOPS
                        ) 
                    }
                    item { 
                        AgentSection(
                            title = "Security", 
                            content = uiState.report.securityAudit, 
                            agent = AgentType.SECURITY,
                            isActive = uiState.currentAgent == AgentType.SECURITY
                        ) 
                    }
                }
            }
        }
    }
}

@Composable
fun AgentSection(
    title: String, 
    content: String, 
    agent: AgentType, 
    isActive: Boolean,
    isCode: Boolean = false
) {
    if (content.isBlank() && !isActive) return

    val cardColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getAgentIcon(agent),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = agent.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.weight(1f))
                if (isActive) {
                    Text(
                        "STREAMING...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (content.isNotEmpty()) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Complete", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (content.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCode) Color(0xFF1E1E1E) else Color.Transparent
                ) {
                    Text(
                        text = content,
                        modifier = Modifier.padding(if (isCode) 12.dp else 0.dp),
                        style = if (isCode) 
                            MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color(0xFFD4D4D4))
                        else 
                            MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            } else if (isActive) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
        }
    }
}

private fun getAgentIcon(agent: AgentType): ImageVector {
    return when (agent) {
        AgentType.PRODUCT_MANAGER -> Icons.Default.Assignment
        AgentType.ARCHITECT -> Icons.Default.Architecture
        AgentType.DEVELOPER -> Icons.Default.Code
        AgentType.DEVOPS -> Icons.Default.CloudQueue
        AgentType.SECURITY -> Icons.Default.Security
        else -> Icons.Default.SmartToy
    }
}
