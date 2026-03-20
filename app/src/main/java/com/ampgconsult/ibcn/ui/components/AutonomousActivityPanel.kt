package com.ampgconsult.ibcn.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampgconsult.ibcn.data.models.*

@Composable
fun AutonomousActivityPanel(
    uiState: com.ampgconsult.ibcn.ui.viewmodels.AutonomousUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()

    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.logs.size - 1)
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoMode, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AUTONOMOUS BUILDER", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (uiState.isRunning) {
                    TextButton(onClick = onCancel) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Current Plan Summary
            uiState.plan?.let { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(plan.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        // Progress
                        val progress = if (plan.steps.isNotEmpty()) {
                            (plan.currentStepIndex.toFloat() + 1) / plan.steps.size
                        } else 0f
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        )
                        
                        Text(
                            "Step ${plan.currentStepIndex + 1} of ${plan.steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Live Execution Logs
            Text("EXECUTION LOG", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF121212))
                    .padding(8.dp)
            ) {
                LazyColumn(state = scrollState) {
                    items(uiState.logs) { log ->
                        LogLine(log)
                    }
                }
            }
            
            if (uiState.status == AutonomousStatus.COMPLETED) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onCancel, // Close the panel
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("DONE")
                }
            }
        }
    }
}

@Composable
fun LogLine(log: AutonomousLog) {
    val color = when (log.type) {
        LogType.INFO -> Color.White
        LogType.SUCCESS -> Color(0xFF4CAF50)
        LogType.ERROR -> Color(0xFFF44336)
        LogType.WARNING -> Color(0xFFFFEB3B)
        LogType.AI_THOUGHT -> Color(0xFF9C27B0)
    }

    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = when (log.type) {
                LogType.AI_THOUGHT -> "🤖"
                LogType.ERROR -> "❌"
                LogType.SUCCESS -> "✅"
                else -> "•"
            },
            modifier = Modifier.width(20.dp),
            fontSize = 10.sp
        )
        Text(
            text = log.message,
            color = color.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        )
    }
}
