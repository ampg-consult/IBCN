package com.ampgconsult.ibcn.modules.freelancer_hub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.modules.freelancer_hub.viewmodels.FreelancerHubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreelancerHubScreen(
    onBack: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToProposals: () -> Unit,
    viewModel: FreelancerHubViewModel = hiltViewModel()
) {
    val recentInvoices by viewModel.recentInvoices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freelancer Business Hub") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Business Management", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HubCard(
                    title = "Clients",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToClients
                )
                HubCard(
                    title = "Invoices",
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToInvoices
                )
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HubCard(
                    title = "Proposals",
                    icon = Icons.Default.Description,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToProposals
                )
                HubCard(
                    title = "Contracts",
                    icon = Icons.Default.Business,
                    modifier = Modifier.weight(1f),
                    onClick = { /* Navigate to Contracts */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recent Invoices", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            if (isLoading && recentInvoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (recentInvoices.isEmpty()) {
                Text("No recent activity.", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.Gray)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(recentInvoices) { invoice ->
                        ListItem(
                            headlineContent = { Text("Invoice #${invoice.id.takeLast(4)}") },
                            supportingContent = { Text("Status: ${invoice.status.name}") },
                            trailingContent = { Text("${invoice.currency} ${invoice.amount}", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}
