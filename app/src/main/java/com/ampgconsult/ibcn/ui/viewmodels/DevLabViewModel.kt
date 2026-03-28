package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.ProjectFile
import com.ampgconsult.ibcn.data.network.AIService
import com.ampgconsult.ibcn.data.repository.ProjectFileService
import com.google.firebase.firestore.FirebaseFirestore
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
    private val aiService: AIService,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevLabUiState())
    val uiState: StateFlow<DevLabUiState> = _uiState.asStateFlow()

    private var currentProjectId: String? = null
    private var streamingJob: Job? = null
    private var fileListenerJob: Job? = null

    fun loadProject(projectId: String) {
        currentProjectId = projectId
        _uiState.update { it.copy(isLoading = true) }
        
        // MODULE 3: REAL-TIME SYNC
        fileListenerJob?.cancel()
        fileListenerJob = viewModelScope.launch {
            firestore.collection("projects").document(projectId).collection("files")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _uiState.update { it.copy(error = error.message) }
                        return@addSnapshotListener
                    }
                    val files = snapshot?.toObjects(ProjectFile::class.java) ?: emptyList()
                    _uiState.update { state ->
                        val updatedActiveFile = files.find { it.id == state.activeFile?.id } ?: files.firstOrNull()
                        state.copy(
                            files = files,
                            isLoading = false,
                            activeFile = updatedActiveFile,
                            editorContent = if (state.isCodeStreaming) state.editorContent else (updatedActiveFile?.content ?: "")
                        )
                    }
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
        if (!_uiState.value.isCodeStreaming) {
            _uiState.update { it.copy(editorContent = newContent) }
        }
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
        }
    }

    fun sendAiRequest() {
        val prompt = _uiState.value.aiPrompt
        if (prompt.isBlank()) return
        _uiState.update { it.copy(aiResponse = "", isAiStreaming = true, aiPrompt = "") }

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            var fullResponse = ""
            aiService.streamResponse(prompt, AgentType.DEVELOPER)
                .catch { e -> _uiState.update { it.copy(error = e.message, isAiStreaming = false) } }
                .collect { chunk ->
                    fullResponse += chunk
                    _uiState.update { it.copy(aiResponse = fullResponse) }
                }
            _uiState.update { it.copy(isAiStreaming = false) }
        }
    }

    fun streamCodeToEditor(prompt: String) {
        val activeFile = _uiState.value.activeFile ?: return
        _uiState.update { it.copy(isCodeStreaming = true) }

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            var fullCode = ""
            aiService.streamResponse(prompt, AgentType.DEVELOPER)
                .catch { e -> _uiState.update { it.copy(error = e.message, isCodeStreaming = false) } }
                .collect { chunk ->
                    fullCode += chunk
                    _uiState.update { it.copy(editorContent = fullCode) }
                }
            _uiState.update { it.copy(isCodeStreaming = false) }
            saveCurrentFile()
        }
    }

    fun applyToIDE(code: String) {
        val currentContent = _uiState.value.editorContent
        val newContent = currentContent + "\n\n" + code
        _uiState.update { it.copy(editorContent = newContent) }
        saveCurrentFile()
    }

    fun triggerAiAction(action: String) {
        when (action) {
            "FIX" -> streamCodeToEditor("Fix bugs in the current file.")
            "REFACTOR" -> streamCodeToEditor("Refactor the current file for performance.")
            "EXPLAIN" -> sendAiRequest()
        }
    }
    
    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.update { it.copy(isAiStreaming = false, isCodeStreaming = false) }
    }
}
