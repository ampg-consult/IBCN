package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.AIAgentOrchestrator
import com.ampgconsult.ibcn.data.models.AgentType
import com.ampgconsult.ibcn.data.models.AIBuilderReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIBuilderUiState(
    val userPrompt: String = "",
    val isProcessing: Boolean = false,
    val currentAgent: AgentType? = null,
    val report: AIBuilderReport = AIBuilderReport(),
    val errorMessage: String? = null,
    val suggestion: String = ""
)

@HiltViewModel
class AIBuilderViewModel @Inject constructor(
    val orchestrator: AIAgentOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIBuilderUiState())
    val uiState: StateFlow<AIBuilderUiState> = _uiState.asStateFlow()

    val suggestion: StateFlow<String> = _uiState.map { it.suggestion }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val isLoading: StateFlow<Boolean> = _uiState.map { it.isProcessing }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onPromptChange(newPrompt: String) {
        _uiState.update { it.copy(userPrompt = newPrompt) }
    }

    fun generateArchitecture(prompt: String) {
        onPromptChange(prompt)
        startOrchestration()
    }

    fun startOrchestration() {
        val prompt = _uiState.value.userPrompt
        if (prompt.isBlank()) return

        _uiState.update { it.copy(isProcessing = true, errorMessage = null, report = AIBuilderReport(), suggestion = "") }

        viewModelScope.launch {
            orchestrator.orchestrate(prompt)
                .onEach { response ->
                    _uiState.update { state ->
                        val updatedReport = when (response.agentType) {
                            AgentType.PRODUCT_MANAGER -> state.report.copy(productPlan = response.content)
                            AgentType.ARCHITECT -> state.report.copy(architecture = response.content)
                            AgentType.DEVELOPER -> state.report.copy(generatedCode = response.content)
                            AgentType.DEVOPS -> state.report.copy(devOpsStrategy = response.content)
                            AgentType.SECURITY -> state.report.copy(securityAudit = response.content)
                            else -> state.report
                        }
                        
                        state.copy(
                            currentAgent = if (response.isComplete) null else response.agentType,
                            report = updatedReport,
                            errorMessage = if (response.isError) response.content else null,
                            suggestion = if (response.isComplete) response.content else state.suggestion
                        )
                    }
                }
                .onCompletion {
                    _uiState.update { it.copy(isProcessing = false, currentAgent = null) }
                }
                .collect()
        }
    }
}
