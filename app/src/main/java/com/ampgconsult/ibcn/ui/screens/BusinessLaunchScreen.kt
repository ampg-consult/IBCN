package com.ampgconsult.ibcn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.ui.viewmodels.BusinessLaunchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessLaunchScreen(
    onBack: () -> Unit,
    onViewProject: (String) -> Unit,
    viewModel: BusinessLaunchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Startup Launchpad", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (!uiState.isLaunching && !uiState.isComplete) {
                LaunchInputForm(
                    name = uiState.name,
                    idea = uiState.idea,
                    onNameChange = viewModel::onNameChange,
                    onIdeaChange = viewModel::onIdeaChange,
                    onLaunch = viewModel::launchStartup
                )
            } else {
                LaunchProgressView(
                    logs = uiState.logs,
                    isComplete = uiState.isComplete,
                    isLaunching = uiState.isLaunching,
                    scrollState = scrollState,
                    onViewProject = { onViewProject("latest") }
                )
            }
        }
    }
}

@Composable
fun LaunchInputForm(
    name: String,
    idea: String,
    onNameChange: (String) -> Unit,
    onIdeaChange: (String) -> Unit,
    onLaunch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.RocketLaunch,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Ready to build your empire?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Enter your startup name and idea. Our AI will build the code, setup SaaS subscriptions, list it on the marketplace, and pitch to investors.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Startup Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = idea,
            onValueChange = onIdeaChange,
            label = { Text("Startup Idea (Detailed)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onLaunch,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = name.isNotBlank() && idea.isNotBlank()
        ) {
            Text("LAUNCH STARTUP NOW", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LaunchProgressView(
    logs: List<com.ampgconsult.ibcn.data.models.AgentResponse>,
    isComplete: Boolean,
    isLaunching: Boolean,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onViewProject: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = if (isComplete) Color.Green else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                LogItem(log)
            }
        }

        if (isComplete) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onViewProject,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("GO TO STARTUP DASHBOARD", fontWeight = FontWeight.Bold)
            }
        } else if (isLaunching) {
            Text(
                "AI is working... This might take a minute as we generate code and legal documents.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun LogItem(log: com.ampgconsult.ibcn.data.models.AgentResponse) {
    val color = when (log.agentType) {
        AgentType.PRODUCT_MANAGER -> Color(0xFF2196F3)
        AgentType.ARCHITECT -> Color(0xFF9C27B0)
        AgentType.DEVELOPER -> Color(0xFFFF9800)
        AgentType.ANALYTICS_LAB -> Color(0xFF4CAF50)
        AgentType.MARKETPLACE_REVIEWER -> Color(0xFFE91E63)
        AgentType.LAUNCHPAD_PLANNER -> Color(0xFF00BCD4)
        else -> Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (log.agentType) {
                    AgentType.DEVELOPER -> Icons.Default.Code
                    AgentType.ANALYTICS_LAB -> Icons.Default.MonetizationOn
                    AgentType.MARKETPLACE_REVIEWER -> Icons.Default.Storefront
                    AgentType.LAUNCHPAD_PLANNER -> Icons.Default.Business
                    else -> Icons.Default.AutoAwesome
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = log.agentType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = log.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
