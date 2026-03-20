package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.SaaSAnalytics
import com.ampgconsult.ibcn.data.models.SaaSProduct
import com.ampgconsult.ibcn.data.repository.SaaSService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaaSDashboardViewModel @Inject constructor(
    private val saasService: SaaSService
) : ViewModel() {

    private val _product = MutableStateFlow<SaaSProduct?>(null)
    val product: StateFlow<SaaSProduct?> = _product.asStateFlow()

    private val _analytics = MutableStateFlow<SaaSAnalytics?>(null)
    val analytics: StateFlow<SaaSAnalytics?> = _analytics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadProductData(productId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            _product.value = saasService.getProduct(productId)
            _analytics.value = saasService.getAnalytics(productId)
            _isLoading.value = false
        }
    }
}
