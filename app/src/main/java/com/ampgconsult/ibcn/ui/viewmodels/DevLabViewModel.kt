package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.ProjectFile
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.repository.ProjectFileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevLabUiState(
    val files: List<ProjectFile> = emptyList(),
    val activeFile: ProjectFile? = null,
    val editorContent: String = "",
    val aiPrompt: String = "",
    val aiResponse: String = "",
    val isAiStreaming: Boolean = false,
    val isCodeStreaming: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DevLabViewModel @Inject constructor(
    private val fileService: ProjectFileService,
    private val aiService: AIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevLabUiState())
    val uiState: StateFlow<DevLabUiState> = _uiState.asStateFlow()

    private var currentProjectId: String? = null
    private var streamingJob: Job? = null

    fun loadProject(projectId: String) {
        currentProjectId = projectId
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val projectFiles = fileService.getProjectFiles(projectId)
            _uiState.update { 
                it.copy(
                    files = projectFiles,
                    isLoading = false,
                    activeFile = projectFiles.firstOrNull(),
                    editorContent = projectFiles.firstOrNull()?.content ?: ""
                )
            }
        }
    }

    fun setActiveFile(file: ProjectFile) {
        _uiState.update { 
            it.copy(
                activeFile = file,
                editorContent = file.content
            )
        }
    }

    fun onContentChange(newContent: String) {
        _uiState.update { it.copy(editorContent = newContent) }
    }

    fun onAiPromptChange(newPrompt: String) {
        _uiState.update { it.copy(aiPrompt = newPrompt) }
    }

    fun saveCurrentFile() {
        val pid = currentProjectId ?: return
        val file = _uiState.value.activeFile ?: return
        val content = _uiState.value.editorContent
        
        viewModelScope.launch {
            fileService.updateFileContent(pid, file.id, content)
            val updatedFiles = fileService.getProjectFiles(pid)
            _uiState.update { it.copy(files = updatedFiles) }
        }
    }

    fun sendAiRequest() {
        val prompt = _uiState.value.aiPrompt
        if (prompt.isBlank()) return

        val activeFile = _uiState.value.activeFile
        val editorContent = _uiState.value.editorContent
        
        val contextPrompt = """
            You are an expert developer assistant in the IBCN Dev Lab.
            
            CURRENT FILE: ${activeFile?.fileName ?: "None"}
            CURRENT CONTENT:
            $editorContent
            
            USER REQUEST: $prompt
            
            Instructions:
            1. If generating code, provide ONLY the code block.
            2. If explaining, be concise.
        """.trimIndent()

        _uiState.update { it.copy(aiResponse = "", isAiStreaming = true, aiPrompt = "") }

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            var fullResponse = ""
            aiService.streamResponse(contextPrompt, AgentType.DEVELOPER)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isAiStreaming = false) }
                }
                .collect { chunk ->
                    fullResponse += chunk
                    _uiState.update { it.copy(aiResponse = fullResponse) }
                }
            _uiState.update { it.copy(isAiStreaming = false) }
        }
    }

    fun streamCodeToEditor(prompt: String) {
        val activeFile = _uiState.value.activeFile ?: return
        val editorContent = _uiState.value.editorContent

        val contextPrompt = """
            Act as an Autonomous Code Streamer. 
            Target File: ${activeFile.fileName}
            Current Code:
            $editorContent
            
            Task: $prompt
            
            Instructions:
            - Return ONLY the updated code for the entire file.
            - Do not use markdown blocks.
            - Stream the code line by line.
        """.trimIndent()

        _uiState.update { it.copy(isCodeStreaming = true) }

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            var fullCode = ""
            aiService.streamResponse(contextPrompt, AgentType.DEVELOPER)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isCodeStreaming = false) }
                }
                .collect { chunk ->
                    fullCode += chunk
                    _uiState.update { it.copy(editorContent = fullCode) }
                }
            _uiState.update { it.copy(isCodeStreaming = false) }
            saveCurrentFile()
        }
    }

    fun applyToIDE(code: String) {
        // Logic to insert snippet into current editor content at cursor or append
        // For simplicity, we replace or append to the current file
        val currentContent = _uiState.value.editorContent
        val newContent = if (currentContent.contains("// AI INSERT HERE")) {
            currentContent.replace("// AI INSERT HERE", code)
        } else {
            currentContent + "\n\n" + code
        }
        _uiState.update { it.copy(editorContent = newContent) }
        saveCurrentFile()
    }

    fun triggerAiAction(action: String) {
        when (action) {
            "FIX" -> streamCodeToEditor("Fix bugs in this file.")
            "REFACTOR" -> streamCodeToEditor("Refactor this file for better performance.")
            "EXPLAIN" -> sendAiRequest() // Explain uses chat panel
            else -> return
        }
    }
    
    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(isAiStreaming = false, isCodeStreaming = false) }
    }
}
