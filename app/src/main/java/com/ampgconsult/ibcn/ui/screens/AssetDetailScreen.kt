package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.ui.viewmodels.MarketplaceViewModel
import com.ampgconsult.ibcn.ui.viewmodels.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    assetId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: MarketplaceViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val paymentState by paymentViewModel.uiState.collectAsState()
    val asset = uiState.assets.find { it.id == assetId }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(paymentState.isSuccess) {
        if (paymentState.isSuccess) {
            asset?.let {
                viewModel.purchaseAsset(it) {
                    showSuccessDialog = true
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success") },
            text = { Text("Asset access granted! You can now download or use this builder asset.") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(asset?.title ?: "Asset Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (asset == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) CircularProgressIndicator()
                else Text("Asset not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(asset.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("by ${asset.authorUsername}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if(asset.price > 0) "$${asset.price} USD" else "FREE",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < asset.rating.toInt()) Color(0xFFFFD700) else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("${asset.rating} (${asset.downloads} downloads)", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(24.dp))

                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        asset.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(Modifier.height(24.dp))
                    Text("Tech Stack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        asset.tags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag) })
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // PART 7 — PAYMENTS (STRIPE USD)
                    Button(
                        onClick = { 
                            if (asset.price > 0) {
                                paymentViewModel.startPayment(asset.price, asset.id, asset.title)
                            } else {
                                viewModel.purchaseAsset(asset) {
                                    showSuccessDialog = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isLoading && !paymentState.isLoading
                    ) {
                        if (uiState.isLoading || paymentState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(if(asset.price > 0) Icons.Default.Payments else Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if(asset.price > 0) "Buy Now ($${asset.price} USD)" else "Download Free", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { 
                            viewModel.startChat(asset.authorUid, asset.id) { chatId ->
                                onNavigateToChat(chatId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chat with Seller")
                    }
                    
                    // PART 10 — REVIEWS SYSTEM PREVIEW
                    Spacer(Modifier.height(32.dp))
                    Text("Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(asset.rating.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Average Rating", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(16.dp))
                            Text("Only buyers can leave reviews.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
