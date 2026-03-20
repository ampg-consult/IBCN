package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.PaymentProvider
import com.ampgconsult.ibcn.data.models.Wallet
import com.ampgconsult.ibcn.data.models.Transaction
import com.ampgconsult.ibcn.data.models.Payout
import com.ampgconsult.ibcn.data.repository.PaymentManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class PaymentUiState(
    val wallet: Wallet? = null,
    val transactions: List<Transaction> = emptyList(),
    val payouts: List<Payout> = emptyList(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isProcessingPayout: Boolean = false
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentManager: PaymentManager,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        loadWalletData()
    }

    fun loadWalletData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val wallet = paymentManager.getWallet()
            if (wallet != null) {
                // Load transactions
                val txSnapshot = firestore.collection("wallet_transactions")
                    .whereEqualTo("uid", wallet.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .get().await()
                val transactions = txSnapshot.toObjects(Transaction::class.java)

                // Load payouts
                val payoutSnapshot = firestore.collection("payouts")
                    .whereEqualTo("uid", wallet.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().await()
                val payouts = payoutSnapshot.toObjects(Payout::class.java)

                _uiState.update { it.copy(
                    wallet = wallet,
                    transactions = transactions,
                    payouts = payouts,
                    isLoading = false
                ) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load wallet") }
            }
        }
    }

    fun startPayment(amount: Double, assetId: String, assetTitle: String) {
        _uiState.update { it.copy(isLoading = true, isSuccess = false, error = null) }
        viewModelScope.launch {
            try {
                // PART 7 — PAYMENTS (STRIPE USD)
                // In production, this calls a Cloud Function to create a PaymentIntent
                val metadata = mapOf("assetId" to assetId, "assetTitle" to assetTitle)
                val result = paymentManager.initializePayment(amount, "USD", PaymentProvider.Stripe, metadata)
                
                result.onSuccess { reference ->
                    // Simulate Stripe Payment Sheet success
                    delay(2000)
                    val verifyResult = paymentManager.verifyPayment(reference)
                    if (verifyResult.isSuccess) {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Payment verification failed") }
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun requestWithdrawal(amount: Double, bankName: String, accountNumber: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isProcessingPayout = true) }
        viewModelScope.launch {
            val result = paymentManager.requestPayout(amount, bankName, accountNumber)
            if (result.isSuccess) {
                loadWalletData()
                onSuccess()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isProcessingPayout = false) }
        }
    }
}
