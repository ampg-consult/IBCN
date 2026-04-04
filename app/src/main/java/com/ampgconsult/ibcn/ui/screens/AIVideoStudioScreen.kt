package com.ampgconsult.ibcn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.ViralVideoMetadata
import com.ampgconsult.ibcn.ui.components.VideoPlayer
import com.ampgconsult.ibcn.ui.viewmodels.VideoStudioStatus
import com.ampgconsult.ibcn.ui.viewmodels.AIVideoStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIVideoStudioScreen(
    onBack: () -> Unit,
    viewModel: AIVideoStudioViewModel = hiltViewModel()
) {
    var prompt by remember { mutableStateOf("") }
    
    // Standardizing types to resolve delegate errors
    val uiState by viewModel.uiState.collectAsState()
    val generatedVideo by viewModel.generatedVideo.collectAsState()
    val isListed by viewModel.isListedInMarketplace.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Video Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { VideoStudioHero() }

            item {
                PromptInputCard(
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    isGenerating = uiState is VideoStudioStatus.Generating,
                    onGenerate = { viewModel.generateVideo(prompt) }
                )
            }

            item {
                AnimatedVisibility(
                    visible = uiState !is VideoStudioStatus.Idle,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    StatusPanel(uiState)
                }
            }

            if (uiState is VideoStudioStatus.Completed) {
                generatedVideo?.let { video ->
                    if (video.videoUrl.isNotEmpty()) {
                        item {
                            VideoPreviewWorkspace(
                                videoUrl = video.videoUrl,
                                title = video.caption.ifEmpty { "Your AI Masterpiece" },
                                isListed = isListed,
                                onDownload = { },
                                onSell = { viewModel.sellVideo() },
                                onMakeViral = { viewModel.makeViral() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPanel(state: VideoStudioStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is VideoStudioStatus.Generating -> {
                    LinearProgressIndicator(
                        progress = state.progress / 100f,
                        modifier = Modifier.fillMaxWidth().clip(CircleShape)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is VideoStudioStatus.Completed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(8.dp))
                        Text("Production Complete! 🎬", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
                is VideoStudioStatus.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun VideoStudioHero() {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
            Text("AI Video Producer", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PromptInputCard(prompt: String, onPromptChange: (String) -> Unit, isGenerating: Boolean, onGenerate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = prompt, onValueChange = onPromptChange, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe your viral promo...") }, minLines = 3, enabled = !isGenerating
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(), enabled = prompt.isNotBlank() && !isGenerating) {
                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Generate Production Video")
            }
        }
    }
}

@Composable
fun VideoPreviewWorkspace(videoUrl: String, title: String, isListed: Boolean, onDownload: () -> Unit, onSell: () -> Unit, onMakeViral: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                VideoPlayer(videoUrl = videoUrl, modifier = Modifier.fillMaxSize())
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                
                if (isListed) {
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(8.dp))
                            Text("Listed in Marketplace ✅", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(onClick = onMakeViral, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))) {
                        Icon(Icons.Default.TrendingUp, null); Spacer(Modifier.width(8.dp)); Text("Optimize for Virality")
                    }
                }
            }
        }
    }
}
