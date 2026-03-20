package com.ampgconsult.ibcn.data.network

import com.ampgconsult.ibcn.data.models.AgentType
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIService @Inject constructor(
    private val openAIClient: OpenAIClient
) {
    /**
     * PRIMARY ENTRY POINT FOR ALL AI REQUESTS (STREAMING)
     * OpenAI is the sole engine.
     */
    fun streamResponse(prompt: String, agentType: AgentType): Flow<String> {
        val systemPrompt = getSystemPrompt(agentType)
        return openAIClient.streamChatCompletion(prompt, systemPrompt)
            .catch { e ->
                emit("AI Error: Connection failed. ${e.message}")
            }
    }

    /**
     * PRIMARY ENTRY POINT FOR ALL AI REQUESTS (UNARY)
     */
    suspend fun getResponse(prompt: String, agentType: AgentType): Result<String> {
        val systemPrompt = getSystemPrompt(agentType)
        return openAIClient.getChatCompletion(prompt, systemPrompt)
    }

    private fun getSystemPrompt(agentType: AgentType): String {
        return when (agentType) {
            AgentType.PRODUCT_MANAGER -> "You are a Senior Product Manager for IBCN. Convert ideas into professional product specs, features, and user stories."
            AgentType.ARCHITECT -> "You are a Senior System Architect for IBCN. Design full scalable architecture, service diagrams, and database schemas."
            AgentType.DEVELOPER -> "You are an expert Senior Software Engineer specializing in Flutter, Android, and Scalable Backend systems. Generate clean, production-ready code."
            AgentType.DEVOPS -> "You are a Senior DevOps Engineer. Create deployment plans, CI/CD pipelines, and infrastructure as code."
            AgentType.SECURITY -> "You are a Senior Security Auditor. Analyze architecture for vulnerabilities and recommend secure best practices."
            AgentType.USERNAME_ASSISTANT -> "You are a creative brand naming assistant. Generate unique, professional, and catchy usernames/brand names."
            AgentType.LAUNCHPAD_PLANNER -> "You are a Startup Strategist and VC Consultant. Create comprehensive MVP roadmaps and monetization strategies."
            AgentType.HABIT_GENERATOR -> "You are a Productivity Coach for builders. Generate actionable, high-impact daily tasks to maintain momentum."
            AgentType.MARKETPLACE_REVIEWER -> "You are a Code Quality and Asset Reviewer. Audit marketplace assets for quality, performance, and best practices."
            AgentType.DOC_GENERATOR -> "You are a Technical Documentation Specialist. Generate clear, comprehensive READMEs and API documentation."
            AgentType.ANALYTICS_LAB -> "You are a Senior Data Analyst and Platform Strategist."
            AgentType.MEDIA_STRATEGIST -> "You are a Viral Media Strategist and Growth Expert."
        }
    }
}
