package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentResponse
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.GeneratedAsset
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.repository.MarketplaceService
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class AIAssetGeneratorService @Inject constructor(
    private val aiService: AIService,
    private val marketplaceService: MarketplaceService
) {
    /**
     * Complete AI Asset Generation Flow:
     * 1. Generate Code
     * 2. Generate Docs
     * 3. Generate Marketing & Pricing
     * 4. Package metadata
     */
    fun generateFullAsset(idea: String): Flow<AgentResponse> = flow {
        // 1. Logic Generation
        emitAll(runAgent(AgentType.DEVELOPER, 
            "Create a production-ready Flutter/Android asset for: $idea. " +
            "Return the full source code for a reusable component or service."))

        // 2. Documentation & SEO
        emitAll(runAgent(AgentType.DOC_GENERATOR, 
            "Create a professional README, setup guide, and SEO-optimized Title and Description for: $idea"))

        // 3. Growth & Pricing Engine
        emitAll(runAgent(AgentType.PRODUCT_MANAGER, 
            "Act as a Marketplace Strategist. Analyze the asset '$idea' and suggest: " +
            "1. Price in USD, 2. Category, 3. Tags, 4. Viral Social Captions for X, LinkedIn and WhatsApp."))
    }

    private fun runAgent(agentType: AgentType, prompt: String): Flow<AgentResponse> = flow {
        var fullContent = ""
        aiService.streamResponse(prompt, agentType)
            .catch { e ->
                emit(AgentResponse(agentType, "Generation Error: ${e.message}", isError = true))
            }
            .collect { chunk ->
                fullContent += chunk
                emit(AgentResponse(agentType, fullContent))
            }
        emit(AgentResponse(agentType, fullContent, isComplete = true))
    }

    suspend fun packageAndPublish(
        idea: String,
        code: String,
        docs: String,
        metadata: String,
        customPrice: Double? = null
    ): Result<String> {
        // Extract pricing and tags from metadata (Mock extraction logic based on expected AI format)
        val priceMatch = Regex("Price:? \\$?(\\d+(\\.\\d+)?)").find(metadata)
        val suggestedPrice = priceMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 19.99
        val finalPrice = customPrice ?: suggestedPrice

        val asset = GeneratedAsset(
            id = UUID.randomUUID().toString(),
            idea = idea,
            code = code,
            readme = docs,
            seoTitle = idea, // In production, extract from docs
            seoDescription = docs.take(200),
            suggestedPrice = suggestedPrice,
            createdAt = Timestamp.now()
        )

        // Publish to marketplace
        return marketplaceService.publishAsset(
            title = asset.seoTitle,
            description = asset.seoDescription,
            price = finalPrice,
            category = "AI Generated",
            techStack = listOf("Flutter", "AI"),
            assetUrl = "internal://assets/${asset.id}" // In production, this points to a ZIP in Storage
        )
    }
}
