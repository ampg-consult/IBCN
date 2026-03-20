package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAIProviderImpl @Inject constructor(
    private val aiModelRouter: AIModelRouter,
    private val localAIManager: LocalAIManager
) : AIProvider {
    override fun streamResponse(prompt: String, agentType: AgentType): Flow<String> {
        return aiModelRouter.streamResponse(prompt, agentType)
    }

    override suspend fun getResponse(prompt: String, agentType: AgentType): Result<String> {
        return aiModelRouter.getResponse(prompt, agentType)
    }

    override suspend fun isAvailable(): Boolean {
        // As per objective: Cloud is primary, always check if either cloud or local is up
        // Since we don't have a reliable cloud health check yet, we assume it is or fallback to local
        return true 
    }
}
