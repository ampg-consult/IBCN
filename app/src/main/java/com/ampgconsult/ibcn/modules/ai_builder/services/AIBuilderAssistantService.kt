package com.ampgconsult.ibcn.modules.ai_builder.services

import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIBuilderAssistantService @Inject constructor(
    private val aiService: AIService
) {
    suspend fun generateCode(prompt: String): Result<String> {
        return aiService.getResponse("Generate high-quality, production-ready code for: $prompt", AgentType.DEVELOPER)
    }

    suspend fun suggestArchitecture(prompt: String): Result<String> {
        return aiService.getResponse("Suggest a scalable and modular architecture for: $prompt", AgentType.ARCHITECT)
    }

    suspend fun debugCode(code: String, error: String): Result<String> {
        return aiService.getResponse("Analyze and fix this code: $code \n Error: $error", AgentType.DEVELOPER)
    }

    suspend fun generateDocumentation(code: String): Result<String> {
        return aiService.getResponse("Generate comprehensive technical documentation for this code: $code", AgentType.DOC_GENERATOR)
    }

    suspend fun explainProject(description: String): Result<String> {
        return aiService.getResponse("Provide a deep-dive explanation of this project: $description", AgentType.PRODUCT_MANAGER)
    }
}
