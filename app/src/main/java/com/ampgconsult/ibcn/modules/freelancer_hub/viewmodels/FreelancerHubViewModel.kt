package com.ampgconsult.ibcn.modules.freelancer_hub.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.modules.freelancer_hub.models.Invoice
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FreelancerHubViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _recentInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val recentInvoices: StateFlow<List<Invoice>> = _recentInvoices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchRecentActivity()
    }

    fun fetchRecentActivity() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            // In a real scenario, we might query across invoices, proposals etc.
            // For now, let's stream the latest invoices
            firestore.collection("freelancer_invoices")
                .whereEqualTo("userId", uid) // Assuming invoices are linked to user
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    _recentInvoices.value = snapshot?.toObjects(Invoice::class.java) ?: emptyList()
                    _isLoading.value = false
                }
        }
    }
}
