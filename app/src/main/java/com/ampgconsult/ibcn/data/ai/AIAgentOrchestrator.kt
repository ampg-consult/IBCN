package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentResponse
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIAgentOrchestrator @Inject constructor(
    private val aiService: AIService
) {
    /**
     * Orchestrates a multi-agent development flow sequentially.
     * Flow: Product Manager -> Architect -> Developer -> DevOps -> Security
     * Uses AIService (OpenAI primary).
     */
    fun orchestrate(userPrompt: String): Flow<AgentResponse> = flow {
        // 1. AI Product Manager
        emitAll(runAgent(AgentType.PRODUCT_MANAGER, 
            "Act as an AI Product Manager. Convert the following idea into product description, " +
            "core features, user stories, and development phases: $userPrompt"))

        // 2. AI Architect
        emitAll(runAgent(AgentType.ARCHITECT, 
            "Act as an AI Architect. Design the system architecture based on the project idea: $userPrompt. " +
            "Provide: architecture overview, service diagram, database structure, and API design."))

        // 3. AI Developer
        emitAll(runAgent(AgentType.DEVELOPER, 
            "Act as an AI Developer. Generate real code (Flutter components, backend APIs, database schemas, " +
            "integration logic) for this project: $userPrompt"))

        // 4. AI DevOps Engineer
        emitAll(runAgent(AgentType.DEVOPS, 
            "Act as an AI DevOps Engineer. Create deployment plans including infrastructure plan, " +
            "CI/CD pipeline, Docker configuration, and scaling strategy for: $userPrompt"))

        // 5. AI Security Engineer
        emitAll(runAgent(AgentType.SECURITY, 
            "Act as an AI Security Engineer. Audit the generated architecture for: security risks, " +
            "authentication design, API protection, and secure storage recommendations for: $userPrompt"))
    }

    private fun runAgent(agentType: AgentType, prompt: String): Flow<AgentResponse> = flow {
        var fullContent = ""
        emit(AgentResponse(agentType, "", isComplete = false))
        
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

    suspend fun getUsernameSuggestions(prompt: String): Result<List<String>> {
        val fullPrompt = "Generate 3-5 unique username suggestions based on: $prompt. " +
                "Rules: 3-20 characters, letters/numbers/underscore, case-insensitive. " +
                "Return ONLY a comma-separated list of suggestions."
        
        return aiService.getResponse(fullPrompt, AgentType.USERNAME_ASSISTANT).map { response ->
            response.split(",").map { it.trim().removePrefix("@") }.filter { it.isNotEmpty() }
        }
    }
}
