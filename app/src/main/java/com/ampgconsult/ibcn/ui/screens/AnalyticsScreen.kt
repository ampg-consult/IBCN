package com.ampgconsult.ibcn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.ActivityEvent
import com.ampgconsult.ibcn.data.models.SystemMetrics
import com.ampgconsult.ibcn.ui.viewmodels.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val metrics by viewModel.metrics.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Platform Health Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchAnalyticsData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading && metrics == null) {
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
                    PlatformStatusHeader(metrics)
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HealthStatCard("Uptime", metrics?.uptime ?: "0%", Icons.Default.Timer, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        HealthStatCard("AI Health", metrics?.aiHealth ?: "N/A", Icons.Default.AutoAwesome, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    }
                }

                item {
                    AutonomousActivityCard(activities)
                }

                item {
                    Text("AI Agent Efficiency", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    AgentPerformanceList(metrics?.agentEfficiencies ?: emptyMap())
                }

                item {
                    ResourceUsageCard(metrics)
                }
            }
        }
    }
}

@Composable
fun PlatformStatusHeader(metrics: SystemMetrics?) {
    val statusColor = when (metrics?.aiHealth?.lowercase()) {
        "optimal", "healthy" -> Color.Green
        "degraded" -> Color.Yellow
        else -> Color.Red
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("IBCN Platform Status", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    text = metrics?.statusMessage ?: "Connecting to IBCN Core...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun HealthStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun AutonomousActivityCard(activities: List<ActivityEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            if (activities.isEmpty()) {
                Text("No recent activity logs.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            } else {
                activities.forEach { event ->
                    ActivityItem(event.text, formatTimestamp(event.timestamp))
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun ActivityItem(text: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        Text(time, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun AgentPerformanceList(efficiencies: Map<String, Float>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (efficiencies.isEmpty()) {
            Text("Awaiting agent data...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        } else {
            efficiencies.forEach { (name, progress) ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                        Text("${(progress * 100).toInt()}% Success", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ResourceUsageCard(metrics: SystemMetrics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Global Resource Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                UsageIndicator("CPU", metrics?.cpuUsage ?: 0f)
                UsageIndicator("RAM", metrics?.ramUsage ?: 0f)
                UsageIndicator("GPU", metrics?.gpuUsage ?: 0f)
            }
        }
    }
}

@Composable
fun UsageIndicator(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
            CircularProgressIndicator(
                progress = { value },
                modifier = Modifier.size(60.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
