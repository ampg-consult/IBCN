package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentType
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIModelRouter @Inject constructor(
    private val cloudAIManager: CloudAIManager,
    private val localAIManager: LocalAIManager
) {
    /**
     * Rules:
     * - Primary: GPT-4 Turbo (Cloud)
     * - Fallback: phi3 (Local) for lightweight tasks or when offline
     * - "Heavy" tasks (Code, Arch, Analytics) -> Always Cloud
     */
    fun streamResponse(prompt: String, agentType: AgentType): Flow<String> {
        return flow {
            try {
                cloudAIManager.streamAIRequest(prompt, getSystemPrompt(agentType))
                    .catch { e ->
                        if (isLightweightTask(agentType) && localAIManager.aiState.value == AIState.LOCAL_AVAILABLE) {
                            emitAll(localAIManager.streamAIRequest(prompt, agentType))
                        } else {
                            emit("AI unavailable. Please try again later.")
                        }
                    }
                    .collect { emit(it) }
            } catch (e: Exception) {
                if (isLightweightTask(agentType) && localAIManager.aiState.value == AIState.LOCAL_AVAILABLE) {
                    emitAll(localAIManager.streamAIRequest(prompt, agentType))
                } else {
                    emit("AI unavailable. Please try again later.")
                }
            }
        }
    }

    suspend fun getResponse(prompt: String, agentType: AgentType): Result<String> {
        val cloudResult = cloudAIManager.requestAI(prompt, getSystemPrompt(agentType))
        if (cloudResult.isSuccess) return cloudResult

        if (isLightweightTask(agentType) && localAIManager.aiState.value == AIState.LOCAL_AVAILABLE) {
            return localAIManager.requestAI(prompt, agentType)
        }

        return Result.failure(cloudResult.exceptionOrNull() ?: Exception("AI unavailable. Please try again later."))
    }

    private fun isLightweightTask(agentType: AgentType): Boolean {
        return when (agentType) {
            AgentType.USERNAME_ASSISTANT -> true
            AgentType.HABIT_GENERATOR -> true
            AgentType.PRODUCT_MANAGER -> false
            AgentType.ARCHITECT -> false
            AgentType.DEVELOPER -> false
            AgentType.DEVOPS -> false
            AgentType.SECURITY -> false
            AgentType.LAUNCHPAD_PLANNER -> false
            AgentType.MARKETPLACE_REVIEWER -> false
            AgentType.DOC_GENERATOR -> false
            AgentType.ANALYTICS_LAB -> false
            AgentType.MEDIA_STRATEGIST -> false
        }
    }

    private fun getSystemPrompt(agentType: AgentType): String {
        return when (agentType) {
            AgentType.PRODUCT_MANAGER -> "You are a Senior Product Manager for IBCN."
            AgentType.ARCHITECT -> "You are a Senior System Architect for IBCN."
            AgentType.DEVELOPER -> "You are a Senior Software Engineer specializing in Flutter and Android."
            AgentType.DEVOPS -> "You are a Senior DevOps Engineer."
            AgentType.SECURITY -> "You are a Senior Security Auditor."
            AgentType.USERNAME_ASSISTANT -> "You are a creative brand naming assistant."
            AgentType.LAUNCHPAD_PLANNER -> "You are a Startup Strategist and VC Consultant."
            AgentType.HABIT_GENERATOR -> "You are a Productivity Coach for builders."
            AgentType.MARKETPLACE_REVIEWER -> "You are a Code Quality and Asset Reviewer."
            AgentType.DOC_GENERATOR -> "You are a Technical Documentation Specialist."
            AgentType.ANALYTICS_LAB -> "You are a Senior Data Analyst and Platform Strategist."
            AgentType.MEDIA_STRATEGIST -> "You are a Viral Media Strategist."
        }
    }
}
