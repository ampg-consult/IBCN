package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.ChatMessage
import com.ampgconsult.ibcn.ui.viewmodels.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val currentUserId = viewModel.currentUserId
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatId) {
        viewModel.setChatId(chatId)
    }

    // Auto-scroll logic: with reverseLayout=true, index 0 is the bottom (latest)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        // PRIMARY FIX: Integrate IME (keyboard) and system bars into the Scaffold's content insets.
        // This ensures the innerPadding provided to the content resizes dynamically when the keyboard opens.
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Collaboration Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Project Workspace", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets.statusBars // Keep TopAppBar at the very top
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) // Consumes system bars and keyboard height automatically
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // 1. Message List (Reverse layout for WhatsApp-style behavior)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true, // Newer messages appear at the bottom
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // messages is assumed to be oldest -> newest. reversed() puts newest at index 0.
                items(messages.reversed()) { message ->
                    MessageBubble(message, isMe = message.senderId == currentUserId)
                }
            }

            // 2. Chat Input Container
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom // Bottom alignment for multiline text
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F3F4),
                            unfocusedContainerColor = Color(0xFFF1F3F4),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send, 
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primary else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (isMe) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
