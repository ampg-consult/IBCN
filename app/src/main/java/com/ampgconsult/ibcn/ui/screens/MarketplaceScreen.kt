package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.MarketplaceAsset
import com.ampgconsult.ibcn.ui.viewmodels.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToVideoStudio: () -> Unit,
    viewModel: MarketplaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Builder Marketplace", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToVideoStudio) {
                        Icon(Icons.Default.MovieCreation, contentDescription = "AI Video Studio", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToPublish) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Publish")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToPublish, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Publish Asset")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchAssets(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search assets...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Trending Assets
                    item {
                        MarketplaceSection("Trending Assets", uiState.assets.take(5), onNavigateToDetail)
                    }

                    // Categories
                    item {
                        CategorySection { category -> viewModel.fetchAssets(category) }
                    }

                    // New Releases
                    item {
                        MarketplaceSection("New Releases", uiState.assets.sortedByDescending { it.createdAt }.take(5), onNavigateToDetail)
                    }

                    // All Assets / Grid
                    item {
                        Text("All Builder Assets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    
                    items(uiState.assets.chunked(2)) { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            pair.forEach { asset ->
                                Box(modifier = Modifier.weight(1f)) {
                                    AssetGridItem(asset, onClick = { onNavigateToDetail(asset.id) })
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceSection(title: String, assets: List<MarketplaceAsset>, onNavigateToDetail: (String) -> Unit) {
    if (assets.isEmpty()) return
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(assets) { asset ->
                AssetCard(asset, onClick = { onNavigateToDetail(asset.id) })
            }
        }
    }
}

@Composable
fun CategorySection(onCategoryClick: (String) -> Unit) {
    val categories = listOf("Flutter UI Kits", "Backend APIs", "AI Prompts", "DevOps Templates", "Database Schemas", "Automation Tools")
    Column {
        Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = false,
                    onClick = { onCategoryClick(category) },
                    label = { Text(category) }
                )
            }
        }
    }
}

@Composable
fun AssetCard(asset: MarketplaceAsset, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(100.dp).fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(asset.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("@${asset.authorUsername}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                    Text(asset.rating.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AssetGridItem(asset: MarketplaceAsset, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(asset.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(asset.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(if(asset.price > 0) "$${asset.price}" else "Free", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
