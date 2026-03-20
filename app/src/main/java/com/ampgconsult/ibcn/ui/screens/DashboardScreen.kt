package com.ampgconsult.ibcn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.Project
import com.ampgconsult.ibcn.data.models.User
import com.ampgconsult.ibcn.ui.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLaunchAIBuilder: () -> Unit,
    onLaunchAICoding: () -> Unit,
    onSelectProject: (String) -> Unit,
    onViewAnalytics: () -> Unit,
    onViewNotifications: () -> Unit,
    onViewMarketplace: () -> Unit,
    onViewProfile: () -> Unit,
    onViewLaunchpad: () -> Unit,
    onLaunchStartup: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val aiResponse by viewModel.quickAiResponse.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()

    var quickQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("B", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("IBCN", style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 2.sp))
                    }
                },
                actions = {
                    IconButton(onClick = onViewNotifications) {
                        BadgedBox(badge = { Badge { Text("0") } }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onViewProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onLaunchAICoding,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "AI Coding Assistant")
                }
                Spacer(Modifier.height(16.dp))
                ExtendedFloatingActionButton(
                    onClick = onLaunchAIBuilder,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    text = { Text("Build with AI") }
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, null) },
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TrendingUp, null) },
                    label = { Text("Analytics") },
                    selected = false,
                    onClick = onViewAnalytics
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.RocketLaunch, null) },
                    label = { Text("Launchpad") },
                    selected = false,
                    onClick = onViewLaunchpad
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Storefront, null) },
                    label = { Text("Market") },
                    selected = false,
                    onClick = onViewMarketplace
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                WelcomeHeader(currentUser?.username_display ?: currentUser?.displayName ?: "Builder")
            }
            
            item {
                QuickStatsRow(currentUser)
            }

            item {
                MegaLaunchBanner(onClick = onLaunchStartup)
            }

            item {
                QuickAskAiCard(
                    query = quickQuery,
                    onQueryChange = { quickQuery = it },
                    onAsk = { 
                        viewModel.quickAskAi(quickQuery)
                        quickQuery = ""
                    },
                    response = aiResponse,
                    isThinking = isAiThinking,
                    onOpenFullAssistant = onLaunchAICoding
                )
            }

            item {
                SectionHeader("Active Projects", onSeeAll = {})
            }

            if (projects.isEmpty()) {
                item {
                    EmptyStateCard(onClick = onLaunchAIBuilder)
                }
            } else {
                items(projects) { project ->
                    HighFidelityProjectCard(project, onClick = { onSelectProject(project.id) })
                }
            }

            item {
                SectionHeader("Global Mapping", onSeeAll = {})
                NetworkMapPlaceholder()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaLaunchBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Launch Full Startup",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Build, Monetize, and Pitch in one AI flow.",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun QuickAskAiCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onAsk: () -> Unit,
    response: String,
    isThinking: Boolean,
    onOpenFullAssistant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("AI Coding Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenFullAssistant) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open Full Assistant", modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask a coding question...", style = MaterialTheme.typography.bodySmall) },
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (isThinking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onAsk, enabled = query.isNotBlank()) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            AnimatedVisibility(
                visible = response.isNotEmpty() || isThinking,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (response.isEmpty() && isThinking) "Thinking..." else response,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("No projects yet", fontWeight = FontWeight.Bold)
            Text("Start building your first AI project", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun WelcomeHeader(name: String) {
    Column {
        Text("Welcome back,", style = MaterialTheme.typography.bodyLarge)
        Text(name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun QuickStatsRow(user: User?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatBox("Active", (user?.projects_count ?: 0).toString(), Icons.Default.RocketLaunch, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        StatBox("Credits", "$${user?.credits ?: 0}", Icons.Default.Token, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        StatBox("Rank", "#${user?.rank ?: "N/A"}", Icons.Default.EmojiEvents, Color(0xFFFFD700), Modifier.weight(1f))
    }
}

@Composable
fun StatBox(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onSeeAll) {
            Text("See All", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun HighFidelityProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(project.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(project.progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            }
            
            Spacer(Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { project.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status:", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(8.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text(project.status, fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Composable
fun NetworkMapPlaceholder() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        val pointColor = MaterialTheme.colorScheme.tertiary
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = listOf(
                center.copy(x = center.x - 100, y = center.y - 30),
                center.copy(x = center.x + 50, y = center.y - 50),
                center.copy(x = center.x - 20, y = center.y + 40),
                center.copy(x = center.x + 120, y = center.y + 20)
            )
            
            points.forEachIndexed { i, p1 ->
                points.forEachIndexed { j, p2 ->
                    if (i < j) drawLine(strokeColor, p1, p2, strokeWidth = 2f)
                }
                drawCircle(pointColor, radius = 6f, center = p1)
            }
        }
        
        Box(contentAlignment = Alignment.Center) {
            Text("Global Activity Node Map", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
