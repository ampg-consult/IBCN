package com.ampgconsult.ibcn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.ai.AIState
import com.ampgconsult.ibcn.data.models.AICodingMessage
import com.ampgconsult.ibcn.data.models.ProjectFile
import com.ampgconsult.ibcn.ui.viewmodels.AICodingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICodingScreen(
    onBack: () -> Unit,
    projectId: String = "",
    viewModel: AICodingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showFileSelector by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        if (projectId.isNotEmpty()) {
            viewModel.setProject(projectId)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AI Coding Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (aiState == AIState.LOCAL_AVAILABLE) "Hybrid AI Active" else "Cloud AI (OpenAI)", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.deployProject() },
                        enabled = !uiState.isTyping,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Deploy", fontSize = 12.sp)
                    }
                    IconButton(onClick = { showFileSelector = !showFileSelector }) {
                        Icon(if (showFileSelector) Icons.Default.FolderOpen else Icons.Default.Folder, contentDescription = "Files")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // Error/Status Banner
            AnimatedVisibility(visible = uiState.error != null) {
                Surface(
                    color = when {
                        uiState.error?.contains("applied", ignoreCase = true) == true -> Color(0xFFE8F5E9)
                        uiState.error?.contains("deployed", ignoreCase = true) == true -> Color(0xFFE3F2FD)
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            when {
                                uiState.error?.contains("applied", ignoreCase = true) == true -> Icons.Default.CheckCircle
                                uiState.error?.contains("deployed", ignoreCase = true) == true -> Icons.Default.RocketLaunch
                                else -> Icons.Default.Error
                            },
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp),
                            tint = when {
                                uiState.error?.contains("applied", ignoreCase = true) == true -> Color(0xFF2E7D32)
                                uiState.error?.contains("deployed", ignoreCase = true) == true -> Color(0xFF1976D2)
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.error ?: "", 
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                uiState.error?.contains("applied", ignoreCase = true) == true -> Color(0xFF2E7D32)
                                uiState.error?.contains("deployed", ignoreCase = true) == true -> Color(0xFF1976D2)
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.weight(1f)) {
                    if (uiState.messages.isEmpty()) {
                        EmptyStatePlaceholder(onSuggestionClick = { viewModel.onInputChange(it); viewModel.sendMessage() })
                    } else {
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.messages) { message ->
                                ChatBubble(
                                    message = message,
                                    onCopyCode = { code ->
                                        clipboardManager.setText(AnnotatedString(code))
                                        scope.launch { snackbarHostState.showSnackbar("Code copied") }
                                    },
                                    onApplyChanges = { code ->
                                        viewModel.applyChanges(code)
                                    }
                                )
                            }
                            
                            if (uiState.isTyping && uiState.messages.lastOrNull()?.role != "assistant") {
                                item { TypingIndicator() }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showFileSelector) {
                    Surface(
                        modifier = Modifier.width(200.dp).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Context Files", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            if (uiState.projectFiles.isEmpty()) {
                                Text("No files.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            } else {
                                LazyColumn {
                                    items(uiState.projectFiles) { file -> FileItem(file) }
                                }
                            }
                        }
                    }
                }
            }

            ChatInputArea(
                userInput = uiState.userInput,
                isProcessing = uiState.isTyping,
                onInputChange = { viewModel.onInputChange(it) },
                onSend = { viewModel.sendMessage() },
                onStop = { viewModel.stopStreaming() }
            )
        }
    }
}

@Composable
fun FileItem(file: ProjectFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = file.fileName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChatInputArea(
    userInput: String,
    isProcessing: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask for code or features...") },
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isProcessing,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F3F4),
                        unfocusedContainerColor = Color(0xFFF1F3F4),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(8.dp))
                if (isProcessing) {
                    IconButton(onClick = onStop, modifier = Modifier.size(48.dp).background(Color.Red.copy(alpha = 0.1f), CircleShape)) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red)
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = userInput.isNotBlank(),
                        modifier = Modifier.size(48.dp).background(if (userInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.Start) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text("Thinking...", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyStatePlaceholder(onSuggestionClick: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            Text("Senior AI Coding Partner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("I have full context of your project. How can I help?", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            
            val suggestions = listOf("Add Firebase Auth", "Create Profile Screen", "Refactor Main widget", "Fix layout issues")
            FlowRow(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(suggestion) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: AICodingMessage, onCopyCode: (String) -> Unit, onApplyChanges: (String) -> Unit) {
    val isUser = message.role == "user"
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val parts = message.content.split("```")
                parts.forEachIndexed { index, part ->
                    if (index % 2 == 1) {
                        val lines = part.lines()
                        val code = if (lines.firstOrNull()?.trim()?.contains(" ") == false) lines.drop(1).joinToString("\n") else part
                        CodeBlock(code = code, onCopy = { onCopyCode(code) }, onApply = { onApplyChanges(code) })
                    } else if (part.isNotBlank()) {
                        SelectionContainer { Text(text = part.trim(), color = if (isUser) Color.White else Color.Black, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, onCopy: () -> Unit, onApply: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E1E1E)).border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))) {
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF333333)).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("GENERATED CODE", color = Color.LightGray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.LightGray) }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onApply, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary) }
            }
        }
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SelectionContainer { Text(text = code.trim(), modifier = Modifier.padding(12.dp), color = Color(0xFFD4D4D4), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)) }
        }
    }
}
