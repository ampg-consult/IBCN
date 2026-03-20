package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.InvestorInsight
import com.ampgconsult.ibcn.data.models.LaunchpadConfig
import com.ampgconsult.ibcn.data.models.StartupProject
import com.ampgconsult.ibcn.ui.viewmodels.StartupLaunchpadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupLaunchpadScreen(
    onBack: () -> Unit,
    viewModel: StartupLaunchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val projects = uiState.projects
    val insights = uiState.insights
    val config = uiState.config
    val isLoading = uiState.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Startup Launchpad", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchLaunchpadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading && projects.isEmpty() && insights.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    LaunchpadHero(config)
                }

                item {
                    Text(
                        "Launch-Ready Projects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (projects.isEmpty()) {
                    item {
                        Text("No projects available for launch yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                } else {
                    items(projects) { project ->
                        LaunchProjectCard(project)
                    }
                }

                if (insights.isNotEmpty()) {
                    item {
                        Text(
                            "Investor Insights",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(insights) { insight ->
                        InvestorInsightCard(insight)
                    }
                }
            }
        }
    }
}

@Composable
fun LaunchpadHero(config: LaunchpadConfig) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = config.heroTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = config.heroDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { /* Handle action */ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(config.heroButtonText, color = MaterialTheme.colorScheme.onTertiary)
                }
            }
        }
    }
}

@Composable
fun LaunchProjectCard(project: StartupProject) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(project.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${project.aiScore}% AI Score",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Market Potential", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(project.marketPotential, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = { /* Launch action */ },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Launch Now")
                }
            }
        }
    }
}

@Composable
fun InvestorInsightCard(insight: InvestorInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(insight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(insight.content, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
