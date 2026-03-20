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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.ChatMessage
import com.ampgconsult.ibcn.data.models.HabitTask
import com.ampgconsult.ibcn.data.models.HabitTaskStatus
import com.ampgconsult.ibcn.ui.components.HabitTaskItem
import com.ampgconsult.ibcn.ui.viewmodels.AIBuilderViewModel
import com.ampgconsult.ibcn.ui.viewmodels.HabitTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    projectId: String,
    projectName: String,
    onBack: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToDevLab: () -> Unit,
    aiViewModel: AIBuilderViewModel = hiltViewModel(),
    habitViewModel: HabitTrackerViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tasks", "Code", "Architecture", "Design", "History", "Agents", "AI Chat")

    LaunchedEffect(projectId) {
        habitViewModel.loadDailyRecord(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color.Green, RoundedCornerShape(50)))
                            Spacer(Modifier.width(4.dp))
                            Text("Live - 3 Collaborators", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDevLab) {
                        Icon(Icons.Default.Terminal, contentDescription = "Open Dev Lab", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { /* Run */ }) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Run", tint = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(onClick = onNavigateToHabits) {
                        Icon(Icons.Default.FactCheck, contentDescription = "Habits")
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> WorkspaceTasksView(projectId, habitViewModel)
                    1 -> CodeEditorView(projectName, onOpenIDE = onNavigateToDevLab)
                    2 -> ArchitectureCanvasView()
                    3 -> DesignWorkspaceView()
                    4 -> VersionControlView()
                    5 -> AgentManagementView()
                    6 -> AIChatView(aiViewModel)
                }
            }
        }
    }
}

@Composable
fun WorkspaceTasksView(projectId: String, viewModel: HabitTrackerViewModel) {
    val dailyRecord by viewModel.dailyRecord.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading && dailyRecord == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("AI Suggested Micro-Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Complete these to maintain your building streak.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            dailyRecord?.tasks?.let { tasks ->
                items(tasks) { task ->
                    HabitTaskItem(
                        task = task,
                        onStatusChange = { newStatus ->
                            viewModel.updateTaskStatus(projectId, task.taskId, newStatus)
                        }
                    )
                }
            }

            if (dailyRecord?.tasks.isNullOrEmpty()) {
                item {
                    Text("No tasks generated for today yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AIChatView(viewModel: AIBuilderViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    
    val chatHistory = remember { mutableStateListOf<ChatMessage>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(chatHistory) { msg ->
                ChatBubble(msg)
            }
            if (uiState.suggestion.isNotEmpty()) {
                item {
                    ChatBubble(ChatMessage(sender = "IBCN AI", text = uiState.suggestion, isUser = false))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask IBCN AI...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        chatHistory.add(ChatMessage(sender = "You", text = messageText, isUser = true))
                        viewModel.generateArchitecture(messageText)
                        messageText = ""
                    }
                },
                enabled = !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = message.sender,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
            color = Color.Gray
        )
    }
}

@Composable
fun CodeEditorView(projectName: String, onOpenIDE: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Main.kt", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenIDE) {
                Text("Open Full IDE", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "package com.ibcn.project\n\nimport androidx.compose.runtime.Composable\n\n@Composable\nfun EntryPoint() {\n    // AI Architect suggested a clean entry point\n    println(\"Project $projectName initialized\")\n}",
                    modifier = Modifier.padding(16.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            
            // AI Suggestion Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(16.dp),
                    onClick = {}
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconHelper(Icons.Default.AutoAwesome, contentDescription = null, size = 16.dp, tint = MaterialTheme.colorScheme.onTertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("Explain Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiary)
                    }
                }
            }
        }
    }
}

@Composable
fun ArchitectureCanvasView() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("System Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconHelper(Icons.Default.AccountTree, contentDescription = null, size = 64.dp, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    Text("Interactive Graph Map Loading...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DesignWorkspaceView() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("UI/UX Workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Mobile Wireframe", style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(
                modifier = Modifier.width(80.dp).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(Modifier.height(16.dp))
                    Icon(Icons.Default.Layers, contentDescription = null)
                    Spacer(Modifier.height(16.dp))
                    Icon(Icons.Default.TextFormat, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun VersionControlView() {
    val logs = listOf(
        "v1.0.4" to "Designer Agent: Updated Primary Colors",
        "v1.0.3" to "Architect Agent: Refactored Auth Logic",
        "v1.0.2" to "User: Added Landing Page content",
        "v1.0.1" to "Developer Agent: Initial Project Scaffold"
    )
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(logs) { (tag, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                ListItem(
                    headlineContent = { Text(tag, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(desc) },
                    trailingContent = {
                        TextButton(onClick = {}) { Text("Rollback", color = MaterialTheme.colorScheme.error) }
                    }
                )
            }
        }
    }
}

@Composable
fun AgentManagementView() {
    val agents = listOf(
        Triple("Architect Agent", "Active - Mapping DB", true),
        Triple("Developer Agent", "Idle", true),
        Triple("Security Agent", "Scanning Dependencies", true),
        Triple("Designer Agent", "Offline", false)
    )
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(agents) { (name, status, active) ->
            ListItem(
                headlineContent = { Text(name) },
                supportingContent = { Text(status) },
                leadingContent = { 
                    IconHelper(
                        if (active) Icons.Default.SmartToy else Icons.Default.CloudOff,
                        contentDescription = null,
                        size = 24.dp,
                        tint = if (active) MaterialTheme.colorScheme.primary else Color.Gray
                    ) 
                },
                trailingContent = { Switch(checked = active, onCheckedChange = {}) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun IconHelper(
    icon: ImageVector,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
}
