package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.AIState
import com.ampgconsult.ibcn.data.ai.LocalAIManager
import com.ampgconsult.ibcn.data.ai.ProjectContextService
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.AICodingMessage
import com.ampgconsult.ibcn.data.models.AICodingUiState
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.repository.DeploymentService
import com.ampgconsult.ibcn.data.repository.ProjectFileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AICodingViewModel @Inject constructor(
    private val aiService: AIService,
    private val localAIManager: LocalAIManager,
    private val projectContextService: ProjectContextService,
    private val fileService: ProjectFileService,
    private val deploymentService: DeploymentService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AICodingUiState())
    val uiState: StateFlow<AICodingUiState> = _uiState.asStateFlow()

    val aiState: StateFlow<AIState> = localAIManager.aiState

    private val conversationHistory = mutableListOf<AICodingMessage>()
    private var currentProjectId: String? = null
    private var streamingJob: Job? = null

    fun setProject(projectId: String) {
        currentProjectId = projectId
        fetchProjectFiles()
    }

    private fun fetchProjectFiles() {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val files = fileService.getProjectFiles(pid)
            _uiState.update { it.copy(projectFiles = files) }
        }
    }

    fun onInputChange(newValue: String) {
        _uiState.update { it.copy(userInput = newValue) }
    }

    fun clearConversation() {
        streamingJob?.cancel()
        conversationHistory.clear()
        _uiState.update { it.copy(messages = emptyList(), isTyping = false, error = null) }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(isTyping = false) }
    }

    fun sendMessage(retryCount: Int = 0) {
        val input = _uiState.value.userInput.trim()
        if (input.isBlank() && retryCount == 0) return

        val messageToSend = if (input.isNotBlank()) input else conversationHistory.lastOrNull { it.role == "user" }?.content ?: ""
        if (messageToSend.isBlank()) return

        if (retryCount == 0) {
            val userMessage = AICodingMessage(role = "user", content = messageToSend)
            conversationHistory.add(userMessage)
            _uiState.update { 
                it.copy(
                    messages = it.messages + userMessage,
                    userInput = "",
                    isTyping = true,
                    error = null
                )
            }
        } else {
            _uiState.update { it.copy(isTyping = true, error = null) }
        }

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            try {
                val projectContext = currentProjectId?.let { 
                    projectContextService.buildProjectContext(it)
                } ?: "No project context."

                val agentType = if (isCodingRequest(messageToSend)) AgentType.DEVELOPER else AgentType.PRODUCT_MANAGER

                val prompt = """
                    PROJECT CONTEXT:
                    $projectContext
                    
                    User Request: $messageToSend
                    
                    Conversation History:
                    ${conversationHistory.takeLast(5).joinToString("\n") { "${it.role}: ${it.content}" }}
                """.trimIndent()

                var fullResponse = ""
                
                if (retryCount == 0 || _uiState.value.messages.lastOrNull()?.role != "assistant") {
                    _uiState.update { it.copy(messages = it.messages + AICodingMessage(role = "assistant", content = "")) }
                }

                aiService.streamResponse(prompt, agentType)
                    .catch { e ->
                        if (retryCount < 2) {
                            delay(1000)
                            sendMessage(retryCount + 1)
                        } else {
                            _uiState.update { it.copy(error = "Connection issue. Please check your internet.", isTyping = false) }
                        }
                    }
                    .collect { chunk ->
                        fullResponse += chunk
                        _uiState.update { state ->
                            val updatedMessages = state.messages.toMutableList()
                            if (updatedMessages.isNotEmpty() && updatedMessages.last().role == "assistant") {
                                updatedMessages[updatedMessages.lastIndex] = AICodingMessage(role = "assistant", content = fullResponse)
                            }
                            state.copy(messages = updatedMessages)
                        }
                    }

                if (fullResponse.isNotEmpty()) {
                    conversationHistory.add(AICodingMessage(role = "assistant", content = fullResponse))
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "AI System failure.", isTyping = false) }
            } finally {
                _uiState.update { it.copy(isTyping = false) }
            }
        }
    }

    fun deployProject() {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTyping = true, error = "Initiating deployment to ibcn.site...") }
            val result = deploymentService.deployProject(pid)
            if (result.isSuccess) {
                val liveUrl = result.getOrThrow()
                _uiState.update { it.copy(isTyping = false, error = "Deployed! Opening: $liveUrl") }
                // In actual UI, we might trigger a browser intent or show a clickable link
            } else {
                _uiState.update { it.copy(isTyping = false, error = "Deployment failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun applyChanges(code: String) {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            val files = fileService.getProjectFiles(pid)
            val targetFile = files.find { it.fileName.endsWith(".dart") }
            
            val result = if (targetFile != null) {
                fileService.updateFileContent(pid, targetFile.id, code)
            } else {
                fileService.uploadFile(pid, "generated_component.dart", code, "lib/")
            }

            if (result.isSuccess) {
                fetchProjectFiles()
                _uiState.update { it.copy(error = "Code applied successfully to IDE") }
                delay(2000)
                _uiState.update { it.copy(error = null) }
            } else {
                _uiState.update { it.copy(error = "Failed to apply changes: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    private fun isCodingRequest(input: String): Boolean {
        val keywords = listOf("build", "code", "flutter", "fix error", "refactor", "function", "class", "ui", "widget", "file", "improve", "create")
        return keywords.any { input.contains(it, ignoreCase = true) }
    }
}
