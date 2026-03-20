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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ampgconsult.ibcn.data.models.Transaction
import com.ampgconsult.ibcn.ui.viewmodels.PaymentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    onNavigateToWithdraw: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadWalletData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.wallet == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    BalanceCard(
                        balance = uiState.wallet?.balance ?: 0.0,
                        currency = uiState.wallet?.currency ?: "USD",
                        onWithdraw = onNavigateToWithdraw
                    )
                }

                item {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions yet", color = Color.Gray)
                        }
                    }
                } else {
                    items(uiState.transactions) { transaction ->
                        TransactionItem(transaction)
                    }
                }
                
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Double, currency: String, onWithdraw: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
        )) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Available Balance", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "$currency ${String.format("%.2f", balance)}",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onWithdraw,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Withdraw Funds")
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: Transaction) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val date = tx.createdAt.toDate()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (tx.type == "credit") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.type == "credit") Icons.Default.Add else Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (tx.type == "credit") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(sdf.format(date), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Text(
                text = "${if (tx.type == "credit") "+" else "-"}$${String.format("%.2f", tx.amount)}",
                fontWeight = FontWeight.ExtraBold,
                color = if (tx.type == "credit") Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
