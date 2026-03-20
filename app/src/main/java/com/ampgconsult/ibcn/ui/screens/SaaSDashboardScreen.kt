package com.ampgconsult.ibcn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.SaaSAnalytics
import com.ampgconsult.ibcn.data.models.SaaSProduct
import com.ampgconsult.ibcn.ui.viewmodels.SaaSDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaaSDashboardScreen(
    productId: String,
    onBack: () -> Unit,
    viewModel: SaaSDashboardViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(productId) {
        viewModel.loadProductData(productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "SaaS Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (product == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Product not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RevenueOverviewCard(product!!)
                }
                item {
                    StatsGrid(product!!, analytics)
                }
                item {
                    Text("Feature Usage & Gating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    FeatureGatingCard(product!!)
                }
                item {
                    Button(
                        onClick = { /* Open Landing Page */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View Landing Page")
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueOverviewCard(product: SaaSProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Total Revenue", style = MaterialTheme.typography.labelMedium)
            Text(
                "$${String.format("%.2f", product.revenueTotal)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("+12% from last month", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun StatsGrid(product: SaaSProduct, analytics: SaaSAnalytics?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatItem(
            label = "Active Subs",
            value = product.activeSubscribers.toString(),
            icon = Icons.Default.People,
            modifier = Modifier.weight(1f)
        )
        StatItem(
            label = "Churn Rate",
            value = "${analytics?.churnRate ?: 0.0}%",
            icon = Icons.Default.TrendingDown,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun FeatureGatingCard(product: SaaSProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            product.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(feature, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
