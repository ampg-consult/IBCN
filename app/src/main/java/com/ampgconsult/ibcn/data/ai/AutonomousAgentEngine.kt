package com.ampgconsult.ibcn.data.ai

import com.ampgconsult.ibcn.data.models.*
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.repository.ProjectFileService
import com.ampgconsult.ibcn.data.repository.ProjectRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutonomousAgentEngine @Inject constructor(
    private val aiService: AIService,
    private val fileService: ProjectFileService,
    private val projectRepository: ProjectRepository,
    private val projectContextService: ProjectContextService,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _logs = MutableStateFlow<List<AutonomousLog>>(emptyList())
    val logs: StateFlow<List<AutonomousLog>> = _logs.asStateFlow()

    private val _currentPlan = MutableStateFlow<AutonomousPlan?>(null)
    val currentPlan: StateFlow<AutonomousPlan?> = _currentPlan.asStateFlow()

    /**
     * Entry point for an autonomous build request.
     */
    fun startAutonomousBuild(projectId: String, userRequest: String): Flow<AutonomousStatus> = flow {
        emit(AutonomousStatus.PLANNING)
        addLog("AI is planning the build steps...", LogType.AI_THOUGHT)

        val projectContext = projectContextService.buildProjectContext(projectId)
        
        val planningPrompt = """
            You are an Autonomous Multi-Step Builder Agent. 
            User Request: $userRequest
            
            PROJECT CONTEXT:
            $projectContext
            
            Instructions:
            1. Analyze the request and the existing project.
            2. Break the task into a sequence of steps.
            3. Action Types: CREATE_FILE, UPDATE_FILE, DELETE_FILE, RUN_COMMAND, INSTALL_PACKAGE.
            4. Return ONLY a structured JSON object:
            {
              "plan_description": "Short summary of the plan",
              "steps": [
                {
                  "action": "CREATE_FILE",
                  "file_path": "lib/new_file.dart",
                  "content": "Full source code",
                  "description": "Creating the new component"
                }
              ]
            }
        """.trimIndent()

        val aiResponse = aiService.getResponse(planningPrompt, AgentType.DEVELOPER)
        
        if (aiResponse.isFailure) {
            addLog("Planning failed: ${aiResponse.exceptionOrNull()?.message}", LogType.ERROR)
            emit(AutonomousStatus.FAILED)
            return@flow
        }

        val planJson = JSONObject(aiResponse.getOrThrow())
        val stepsArray = planJson.getJSONArray("steps")
        val steps = mutableListOf<AutonomousStep>()
        
        for (i in 0 until stepsArray.length()) {
            val stepObj = stepsArray.getJSONObject(i)
            steps.add(AutonomousStep(
                id = java.util.UUID.randomUUID().toString(),
                action = AgentActionType.valueOf(stepObj.getString("action")),
                filePath = stepObj.optString("file_path"),
                content = stepObj.optString("content"),
                command = stepObj.optString("command"),
                description = stepObj.getString("description"),
                status = StepStatus.PENDING
            ))
        }

        val plan = AutonomousPlan(
            id = java.util.UUID.randomUUID().toString(),
            projectId = projectId,
            userId = auth.currentUser?.uid ?: "",
            description = planJson.getString("plan_description"),
            steps = steps,
            status = AutonomousStatus.EXECUTING
        )
        
        _currentPlan.value = plan
        emit(AutonomousStatus.EXECUTING)

        // Execution Loop
        for (index in steps.indices) {
            val currentStep = steps[index]
            updateStepStatus(index, StepStatus.IN_PROGRESS)
            addLog("Executing: ${currentStep.description}", LogType.INFO)

            val success = executeStep(projectId, currentStep)
            
            if (success) {
                updateStepStatus(index, StepStatus.SUCCESS)
                addLog("Step ${index + 1} completed successfully.", LogType.SUCCESS)
            } else {
                updateStepStatus(index, StepStatus.FAILED)
                addLog("Step ${index + 1} failed.", LogType.ERROR)
                emit(AutonomousStatus.FAILED)
                return@flow
            }
        }

        _currentPlan.update { it?.copy(status = AutonomousStatus.COMPLETED) }
        emit(AutonomousStatus.COMPLETED)
        addLog("Autonomous build completed successfully! 🚀", LogType.SUCCESS)
    }

    private suspend fun executeStep(projectId: String, step: AutonomousStep): Boolean {
        return try {
            when (step.action) {
                AgentActionType.CREATE_FILE -> {
                    fileService.uploadFile(projectId, step.filePath.substringAfterLast("/"), step.content, step.filePath.substringBeforeLast("/", ""))
                    true
                }
                AgentActionType.UPDATE_FILE -> {
                    // Logic to find file ID by path then update
                    val files = fileService.getProjectFiles(projectId)
                    val targetFile = files.find { "${it.path}${it.fileName}" == step.filePath }
                    if (targetFile != null) {
                        fileService.updateFileContent(projectId, targetFile.id, step.content)
                        true
                    } else false
                }
                AgentActionType.DELETE_FILE -> true // Simulated
                AgentActionType.RUN_COMMAND, AgentActionType.INSTALL_PACKAGE -> {
                    // Simulate command execution in the cloud env
                    addLog("Running command: ${step.command}", LogType.INFO)
                    kotlinx.coroutines.delay(1000)
                    true
                }
                AgentActionType.CONNECT_API -> true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun addLog(message: String, type: LogType) {
        _logs.update { it + AutonomousLog(message, type) }
    }

    private fun updateStepStatus(index: Int, status: StepStatus) {
        _currentPlan.update { plan ->
            val updatedSteps = plan?.steps?.toMutableList() ?: return@update null
            if (index < updatedSteps.size) {
                updatedSteps[index] = updatedSteps[index].copy(status = status)
            }
            plan.copy(steps = updatedSteps, currentStepIndex = index)
        }
    }
}
