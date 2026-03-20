package com.ampgconsult.ibcn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.ProjectFile
import com.ampgconsult.ibcn.ui.components.AutonomousActivityPanel
import com.ampgconsult.ibcn.ui.viewmodels.DevLabAutonomousViewModel
import com.ampgconsult.ibcn.ui.viewmodels.DevLabViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevLabScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: DevLabViewModel = hiltViewModel(),
    autoViewModel: DevLabAutonomousViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val autoUiState by autoViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    var showAiAssistant by remember { mutableStateOf(true) }
    var assistantMode by remember { mutableStateOf(0) } // 0: Chat, 1: Autonomous

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Dev Lab IDE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (uiState.isCodeStreaming) {
                            Text("AI is coding...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(uiState.activeFile?.fileName ?: "No file selected", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isCodeStreaming || uiState.isAiStreaming) {
                        IconButton(onClick = { viewModel.stopStreaming() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red)
                        }
                    }
                    IconButton(onClick = { viewModel.saveCurrentFile() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { /* Launch Web Preview */ }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = Color(0xFF4CAF50))
                    }
                    IconButton(onClick = { showAiAssistant = !showAiAssistant }) {
                        Icon(
                            Icons.Default.AutoAwesome, 
                            contentDescription = "AI Assistant", 
                            tint = if (showAiAssistant) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // LEFT PANEL: File Explorer
            FileExplorerPanel(
                files = uiState.files,
                selectedFile = uiState.activeFile,
                onFileSelected = { file -> viewModel.setActiveFile(file) },
                modifier = Modifier.width(200.dp).fillMaxHeight()
            )

            // CENTER PANEL: Code Editor with Live AI Streaming
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1E1E1E))) {
                if (uiState.activeFile != null) {
                    CodeEditorPanel(
                        content = uiState.editorContent,
                        onContentChange = { viewModel.onContentChange(it) },
                        isStreaming = uiState.isCodeStreaming
                    )
                } else {
                    EmptyEditorPlaceholder()
                }
            }

            // RIGHT PANEL: Dual-Mode AI Assistant
            AnimatedVisibility(
                visible = showAiAssistant,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Column {
                        AssistantTabRow(
                            selectedMode = assistantMode,
                            onModeChange = { assistantMode = it },
                            isAutoRunning = autoUiState.isRunning
                        )

                        if (assistantMode == 0) {
                            AiChatPanel(
                                uiState = uiState,
                                onPromptChange = { viewModel.onAiPromptChange(it) },
                                onSend = { viewModel.sendAiRequest() },
                                onAction = { viewModel.triggerAiAction(it) },
                                onApplyToIDE = { viewModel.applyToIDE(it) },
                                onCopy = { 
                                    clipboardManager.setText(AnnotatedString(it))
                                }
                            )
                        } else {
                            AutonomousPanel(
                                projectId = projectId,
                                autoUiState = autoUiState,
                                onInputChange = { autoViewModel.onInputChange(it) },
                                onStart = { autoViewModel.startBuild(projectId) },
                                onCancel = { assistantMode = 0 }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileExplorerPanel(
    files: List<ProjectFile>,
    selectedFile: ProjectFile?,
    onFileSelected: (ProjectFile) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "FILES", 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn {
                items(files) { file ->
                    FileExplorerItem(
                        file = file,
                        isSelected = file == selectedFile,
                        onClick = { onFileSelected(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun FileExplorerItem(
    file: ProjectFile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.fileName.endsWith(".kt")) Icons.Default.Description else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = file.fileName,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            maxLines = 1
        )
    }
}

@Composable
fun AssistantTabRow(selectedMode: Int, onModeChange: (Int) -> Unit, isAutoRunning: Boolean) {
    TabRow(
        selectedTabIndex = selectedMode,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {}
    ) {
        Tab(selected = selectedMode == 0, onClick = { onModeChange(0) }) {
            Text("AI Chat", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
        }
        Tab(selected = selectedMode == 1, onClick = { onModeChange(1) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAutoRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(4.dp))
                }
                Text("Autonomous", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun CodeEditorPanel(
    content: String,
    onContentChange: (String) -> Unit,
    isStreaming: Boolean
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Auto-scroll to bottom during streaming
    LaunchedEffect(content.length) {
        if (isStreaming) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).horizontalScroll(horizontalScrollState)) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFD4D4D4),
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                enabled = !isStreaming,
                decorationBox = { innerTextField ->
                    Row {
                        // Line numbers could be added here
                        innerTextField()
                    }
                }
            )
        }
        
        if (isStreaming) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AiChatPanel(
    uiState: com.ampgconsult.ibcn.ui.viewmodels.DevLabUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onAction: (String) -> Unit,
    onApplyToIDE: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // AI Thinking / Output Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF252526))
                .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            LazyColumn {
                item {
                    val parts = uiState.aiResponse.split("```")
                    parts.forEachIndexed { index, part ->
                        if (index % 2 == 1) {
                            // Code block in chat
                            IDECodeBlock(
                                code = part.trim(), 
                                onApply = { onApplyToIDE(it) },
                                onCopy = { onCopy(it) }
                            )
                        } else if (part.isNotBlank()) {
                            Text(
                                text = part.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                    if (uiState.isAiStreaming) {
                        Text("▋", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IDEActionButton("Fix", Icons.Default.BugReport, { onAction("FIX") }, Modifier.weight(1f))
            IDEActionButton("Refactor", Icons.Default.History, { onAction("REFACTOR") }, Modifier.weight(1f))
            IDEActionButton("Explain", Icons.Default.Info, { onAction("EXPLAIN") }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        // Input Area
        OutlinedTextField(
            value = uiState.aiPrompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask AI assistant...", fontSize = 12.sp) },
            trailingIcon = {
                IconButton(onClick = onSend, enabled = !uiState.isAiStreaming && uiState.aiPrompt.isNotBlank()) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 13.sp)
        )
    }
}

@Composable
fun IDECodeBlock(code: String, onApply: (String) -> Unit, onCopy: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF333333)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI SUGGESTION", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Row {
                IconButton(onClick = { onCopy(code) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onApply(code) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Text(
                text = code,
                modifier = Modifier.padding(12.dp),
                color = Color(0xFFD4D4D4),
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun IDEActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
        }
    }
}

@Composable
fun AutonomousPanel(
    projectId: String,
    autoUiState: com.ampgconsult.ibcn.ui.viewmodels.AutonomousUiState,
    onInputChange: (String) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (autoUiState.status == com.ampgconsult.ibcn.data.models.AutonomousStatus.IDLE || 
            autoUiState.status == com.ampgconsult.ibcn.data.models.AutonomousStatus.COMPLETED) {
            AutonomousInputArea(
                userInput = autoUiState.userInput,
                onInputChange = onInputChange,
                onStart = onStart,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        if (autoUiState.status != com.ampgconsult.ibcn.data.models.AutonomousStatus.IDLE) {
            AutonomousActivityPanel(
                uiState = autoUiState,
                onCancel = onCancel,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

@Composable
fun AutonomousInputArea(
    userInput: String,
    onInputChange: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "What should the agent build?", 
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = userInput,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Implement a basic onboarding flow with 3 steps...", fontSize = 12.sp) },
            minLines = 3,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            enabled = userInput.isNotBlank(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Launch Autonomous Agent")
        }
    }
}

@Composable
fun EmptyEditorPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Code, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.2f))
            Text("Select a file from the explorer", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
