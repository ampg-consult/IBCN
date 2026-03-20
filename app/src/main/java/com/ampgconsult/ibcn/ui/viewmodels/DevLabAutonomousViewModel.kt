package com.ampgconsult.ibcn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.data.ai.AutonomousAgentEngine
import com.ampgconsult.ibcn.data.models.AutonomousLog
import com.ampgconsult.ibcn.data.models.AutonomousPlan
import com.ampgconsult.ibcn.data.models.AutonomousStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AutonomousUiState(
    val status: AutonomousStatus = AutonomousStatus.IDLE,
    val plan: AutonomousPlan? = null,
    val logs: List<AutonomousLog> = emptyList(),
    val userInput: String = "",
    val isRunning: Boolean = false
)

@HiltViewModel
class DevLabAutonomousViewModel @Inject constructor(
    private val agentEngine: AutonomousAgentEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutonomousUiState())
    val uiState: StateFlow<AutonomousUiState> = _uiState.asStateFlow()

    init {
        // Collect logs and plan from engine
        viewModelScope.launch {
            agentEngine.logs.collect { newLogs ->
                _uiState.update { it.copy(logs = newLogs) }
            }
        }
        viewModelScope.launch {
            agentEngine.currentPlan.collect { newPlan ->
                _uiState.update { it.copy(plan = newPlan) }
            }
        }
    }

    fun onInputChange(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }

    fun startBuild(projectId: String) {
        val input = _uiState.value.userInput
        if (input.isBlank()) return

        _uiState.update { it.copy(isRunning = true, userInput = "") }

        viewModelScope.launch {
            agentEngine.startAutonomousBuild(projectId, input)
                .onEach { status ->
                    _uiState.update { it.copy(status = status) }
                }
                .onCompletion {
                    _uiState.update { it.copy(isRunning = false) }
                }
                .collect()
        }
    }
}
