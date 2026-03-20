package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.AgentResponse
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.network.AIService
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIBuilderOrchestrator @Inject constructor(
    private val aiService: AIService
) {
    /**
     * Orchestrates a full AI Builder session with structured output.
     */
    fun orchestrateBuilder(userPrompt: String): Flow<AgentResponse> = flow {
        // 1. Architecture Design
        emitAll(runAgent(AgentType.ARCHITECT, 
            "Design a full architecture for this project: $userPrompt. " +
            "Provide: Architecture Overview, Technology Stack, System Components, Database Schema, API Endpoints."))

        // 2. Code Generation
        emitAll(runAgent(AgentType.DEVELOPER, 
            "Generate Flutter and Backend code snippets for the core components of: $userPrompt"))

        // 3. DevOps Strategy
        emitAll(runAgent(AgentType.DEVOPS, 
            "Create a Deployment Strategy and CI/CD pipeline for: $userPrompt"))

        // 4. Security Audit
        emitAll(runAgent(AgentType.SECURITY, 
            "Provide Security Considerations and risk assessment for: $userPrompt"))
    }

    /**
     * Generates a startup plan for the Launchpad module.
     */
    fun generateLaunchpadPlan(idea: String): Flow<AgentResponse> = flow {
        emitAll(runAgent(AgentType.LAUNCHPAD_PLANNER, 
            "Create a comprehensive startup plan for: $idea. " +
            "Include: MVP Roadmap, Feature Breakdown, Development Phases, and Monetization Strategies."))
    }

    /**
     * Generates daily habits/tasks based on project description and stage.
     */
    suspend fun generateHabits(projectDescription: String, stage: String): Result<List<String>> {
        val prompt = "Act as a Project Manager. Generate 5 daily actionable tasks for a developer working on: $projectDescription. " +
                "The project is currently in the $stage stage. Return only a comma-separated list of tasks."
        
        return aiService.getResponse(prompt, AgentType.HABIT_GENERATOR).map { response ->
            response.split(",").map { it.trim().removePrefix("- ").removePrefix("• ") }.filter { it.isNotEmpty() }
        }
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
