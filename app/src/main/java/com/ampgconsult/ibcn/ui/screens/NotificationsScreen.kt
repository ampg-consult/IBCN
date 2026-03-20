package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.Notification
import com.ampgconsult.ibcn.ui.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchNotifications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading && notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No notifications yet.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                items(notifications) { notification ->
                    NotificationListItem(
                        notification = notification,
                        onRead = { viewModel.markAsRead(notification.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun NotificationListItem(notification: Notification, onRead: () -> Unit) {
    val icon = when (notification.type) {
        "message" -> Icons.AutoMirrored.Filled.Message
        else -> Icons.Default.Info
    }

    ListItem(
        headlineContent = { 
            Text(
                text = notification.title,
                color = if (notification.isRead) Color.Gray else MaterialTheme.colorScheme.onBackground
            ) 
        },
        supportingContent = { Text(notification.message) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (notification.isRead) Color.Gray else MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { onRead() }
    )
}
