package com.ampgconsult.ibcn.modules.workspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampgconsult.ibcn.modules.workspace.models.KanbanTask
import com.ampgconsult.ibcn.modules.workspace.models.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanBoardScreen(
    projectName: String,
    tasks: List<KanbanTask>,
    onBack: () -> Unit,
    onAddTask: (TaskStatus) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$projectName - Kanban") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        val statuses = TaskStatus.values()
        
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(statuses) { status ->
                KanbanColumn(
                    status = status,
                    tasks = tasks.filter { it.status == status },
                    onAddTask = { onAddTask(status) }
                )
            }
        }
    }
}

@Composable
fun KanbanColumn(
    status: TaskStatus,
    tasks: List<KanbanTask>,
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        tasks.forEach { task ->
            KanbanTaskCard(task)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun KanbanTaskCard(task: KanbanTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = task.priority.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
