package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentResponse
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceOrchestrator @Inject constructor(
    private val aiService: AIService
) {
    /**
     * Generates a description and documentation for a marketplace asset.
     */
    fun generateAssetDescription(title: String, codeSnippet: String): Flow<AgentResponse> = flow {
        emitAll(runAgent(AgentType.DOC_GENERATOR, 
            "Generate a professional marketplace description and documentation for an asset titled '$title'. " +
            "The core logic is: $codeSnippet. " +
            "Include: Overview, Key Features, How to use, and Category suggestions."))
    }

    /**
     * Performs a code review and security scan on a marketplace asset.
     */
    fun reviewAsset(title: String, codeSnippet: String): Flow<AgentResponse> = flow {
        // 1. Code Review
        emitAll(runAgent(AgentType.MARKETPLACE_REVIEWER, 
            "Perform a deep code review for the asset '$title'. Evaluate performance, scalability, and code quality: $codeSnippet"))

        // 2. Security Scan
        emitAll(runAgent(AgentType.SECURITY, 
            "Analyze the security risks and potential vulnerabilities in the following code for the marketplace asset '$title': $codeSnippet"))
    }

    private fun runAgent(agentType: AgentType, prompt: String): Flow<AgentResponse> = flow {
        var fullContent = ""
        aiService.streamResponse(prompt, agentType)
            .catch { e ->
                emit(AgentResponse(agentType, "AI Error: ${e.message ?: "Connection failed."}", isError = true))
            }
            .collect { chunk ->
                fullContent += chunk
                emit(AgentResponse(agentType, fullContent))
            }
        emit(AgentResponse(agentType, fullContent, isComplete = true))
    }
}
