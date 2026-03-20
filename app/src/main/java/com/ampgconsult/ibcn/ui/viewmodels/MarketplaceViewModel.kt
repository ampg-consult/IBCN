package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.MarketplaceAsset
import com.ampgconsult.ibcn.data.repository.MarketplaceService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class MarketplaceUiState(
    val assets: List<MarketplaceAsset> = emptyList(),
    val isLoading: Boolean = false,
    val isPublishing: Boolean = false,
    val error: String? = null,
    val aiSuggestion: Map<String, String>? = null
)

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val marketplaceService: MarketplaceService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        fetchAssets()
    }

    fun fetchAssets(category: String = "All") {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val query = marketplaceService.getAssetsQuery(if (category == "All") null else category)
                val snapshot = query.get().await()
                val assets = snapshot.toObjects(MarketplaceAsset::class.java)
                _uiState.update { it.copy(assets = assets, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun searchAssets(query: String) {
        if (query.isBlank()) {
            fetchAssets()
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val allAssetsQuery = marketplaceService.getAssetsQuery()
                val snapshot = allAssetsQuery.get().await()
                val filtered = snapshot.toObjects(MarketplaceAsset::class.java).filter {
                    it.title.contains(query, ignoreCase = true) || 
                    it.category.contains(query, ignoreCase = true)
                }
                _uiState.update { it.copy(assets = filtered, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun getAiSuggestions(title: String, techStack: String) {
        viewModelScope.launch {
            val result = marketplaceService.getAIEnhancedContent(title, techStack)
            if (result.isSuccess) {
                _uiState.update { it.copy(aiSuggestion = result.getOrNull()) }
            }
        }
    }

    fun publishAsset(
        title: String,
        description: String,
        price: Double,
        category: String,
        techStack: List<String>,
        assetUrl: String
    ) {
        _uiState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            val result = marketplaceService.publishAsset(
                title, description, price, category, techStack, assetUrl
            )
            if (result.isSuccess) {
                fetchAssets()
                _uiState.update { it.copy(isPublishing = false, aiSuggestion = null) }
            } else {
                _uiState.update { it.copy(isPublishing = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun purchaseAsset(asset: MarketplaceAsset, onSuccess: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.update { it.copy(error = "Please sign in to purchase assets") }
            return
        }

        if (currentUser.uid == asset.authorUid) {
            _uiState.update { it.copy(error = "You cannot purchase your own asset") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = marketplaceService.processPurchase(asset)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                onSuccess()
            } else {
                _uiState.update { it.copy(error = "Purchase failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun startChat(sellerId: String, assetId: String, onChatStarted: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.update { it.copy(error = "Please sign in to chat with the seller") }
            return
        }

        if (currentUser.uid == sellerId) {
            _uiState.update { it.copy(error = "You cannot chat with yourself") }
            return
        }

        viewModelScope.launch {
            try {
                val chatId = marketplaceService.startChat(sellerId, assetId)
                onChatStarted(chatId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to start chat: ${e.message}") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
