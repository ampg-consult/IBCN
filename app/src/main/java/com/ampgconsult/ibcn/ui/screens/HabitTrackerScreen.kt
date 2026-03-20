package com.ampgconsult.ibcn.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.HabitTaskStatus
import com.ampgconsult.ibcn.ui.components.HabitTaskItem
import com.ampgconsult.ibcn.ui.viewmodels.HabitTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: HabitTrackerViewModel = hiltViewModel()
) {
    val dailyRecord by viewModel.dailyRecord.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadDailyRecord(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Builder Habits", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StreakBanner(dailyRecord?.streakCount ?: 0)
                }

                item {
                    Text(
                        "Today's Micro-Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                dailyRecord?.tasks?.let { tasks ->
                    items(tasks) { task ->
                        HabitTaskItem(
                            task = task,
                            onStatusChange = { newStatus: HabitTaskStatus ->
                                viewModel.updateTaskStatus(projectId, task.taskId, newStatus)
                            }
                        )
                    }
                }

                item {
                    ReputationProgressCard(dailyRecord?.totalPoints ?: 0)
                }
            }
        }
    }
}

@Composable
fun StreakBanner(streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "$streak Day Streak!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Keep building to unlock AI perks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ReputationProgressCard(points: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Reputation Gained Today", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$points", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
