package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.BusinessOrchestrator
import com.ampgconsult.ibcn.data.models.AgentResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BusinessLaunchUiState(
    val name: String = "",
    val idea: String = "",
    val isLaunching: Boolean = false,
    val logs: List<AgentResponse> = emptyList(),
    val currentStep: String = "",
    val isComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BusinessLaunchViewModel @Inject constructor(
    private val businessOrchestrator: BusinessOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessLaunchUiState())
    val uiState: StateFlow<BusinessLaunchUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onIdeaChange(idea: String) {
        _uiState.update { it.copy(idea = idea) }
    }

    fun launchStartup() {
        val name = _uiState.value.name
        val idea = _uiState.value.idea
        if (name.isBlank() || idea.isBlank()) return

        _uiState.update { it.copy(isLaunching = true, logs = emptyList(), error = null, isComplete = false) }

        viewModelScope.launch {
            businessOrchestrator.launchFullStartup(name, idea)
                .onEach { response ->
                    _uiState.update { state ->
                        state.copy(
                            logs = state.logs + response,
                            currentStep = response.content,
                            isComplete = response.isComplete && response.agentType == com.ampgconsult.ibcn.data.models.AgentType.LAUNCHPAD_PLANNER
                        )
                    }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLaunching = false) }
                }
                .onCompletion {
                    _uiState.update { it.copy(isLaunching = false) }
                }
                .collect()
        }
    }
}
