package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.AIAssetGeneratorService
import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.repository.GrowthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrowthUiState(
    val trendingInsights: List<TrendingInsight> = emptyList(),
    val referralLink: String = "",
    val topEarners: List<LeaderboardEntry> = emptyList(),
    val isGenerating: Boolean = false,
    val generationProgress: AgentResponse? = null,
    val shareContent: String? = null,
    val error: String? = null
)

@HiltViewModel
class GrowthViewModel @Inject constructor(
    private val growthService: GrowthService,
    private val aiGenerator: AIAssetGeneratorService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GrowthUiState())
    val uiState: StateFlow<GrowthUiState> = _uiState.asStateFlow()

    init {
        loadGrowthData()
    }

    private fun loadGrowthData() {
        viewModelScope.launch {
            val link = growthService.getReferralLink()
            _uiState.update { it.copy(referralLink = link) }
            
            growthService.generateTrendingInsights().onSuccess { insights ->
                _uiState.update { it.copy(trendingInsights = insights) }
            }
        }
    }

    /**
     * AI INSTANT ASSET GENERATOR & SELL ENGINE
     */
    fun generateAndSell(idea: String) {
        _uiState.update { it.copy(isGenerating = true, error = null) }
        
        viewModelScope.launch {
            var finalCode = ""
            var finalDocs = ""
            var finalMeta = ""

            aiGenerator.generateFullAsset(idea)
                .onEach { response ->
                    _uiState.update { it.copy(generationProgress = response) }
                    when (response.agentType) {
                        AgentType.DEVELOPER -> finalCode = response.content
                        AgentType.DOC_GENERATOR -> finalDocs = response.content
                        AgentType.PRODUCT_MANAGER -> finalMeta = response.content
                        else -> {}
                    }
                }
                .onCompletion {
                    if (finalCode.isNotEmpty()) {
                        publishGeneratedAsset(idea, finalCode, finalDocs, finalMeta)
                    }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = "Generation failed: ${e.message}", isGenerating = false) }
                }
                .collect()
        }
    }

    private suspend fun publishGeneratedAsset(idea: String, code: String, docs: String, meta: String) {
        aiGenerator.packageAndPublish(idea, code, docs, meta)
            .onSuccess { assetId ->
                _uiState.update { it.copy(isGenerating = false) }
                generateShareAssets(assetId)
            }
            .onFailure { e ->
                _uiState.update { it.copy(error = "Publishing failed: ${e.message}", isGenerating = false) }
            }
    }

    private fun generateShareAssets(assetId: String) {
        viewModelScope.launch {
            growthService.generateShareContent(assetId).onSuccess { content ->
                _uiState.update { it.copy(shareContent = content["content"]) }
            }
        }
    }
}
