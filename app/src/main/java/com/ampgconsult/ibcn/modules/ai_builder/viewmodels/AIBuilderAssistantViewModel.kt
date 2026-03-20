package com.ampgconsult.ibcn.modules.ai_builder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampgconsult.ibcn.modules.ai_builder.services.AIBuilderAssistantService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIBuilderAssistantViewModel @Inject constructor(
    private val assistantService: AIBuilderAssistantService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIBuilderUiState>(AIBuilderUiState.Idle)
    val uiState: StateFlow<AIBuilderUiState> = _uiState

    fun onGenerateCode(prompt: String) {
        viewModelScope.launch {
            _uiState.value = AIBuilderUiState.Loading
            assistantService.generateCode(prompt).fold(
                onSuccess = { _uiState.value = AIBuilderUiState.Success(it) },
                onFailure = { _uiState.value = AIBuilderUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun onSuggestArchitecture(prompt: String) {
        viewModelScope.launch {
            _uiState.value = AIBuilderUiState.Loading
            assistantService.suggestArchitecture(prompt).fold(
                onSuccess = { _uiState.value = AIBuilderUiState.Success(it) },
                onFailure = { _uiState.value = AIBuilderUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }
    
    fun onDebugCode(code: String, error: String) {
        viewModelScope.launch {
            _uiState.value = AIBuilderUiState.Loading
            assistantService.debugCode(code, error).fold(
                onSuccess = { _uiState.value = AIBuilderUiState.Success(it) },
                onFailure = { _uiState.value = AIBuilderUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}

sealed class AIBuilderUiState {
    object Idle : AIBuilderUiState()
    object Loading : AIBuilderUiState()
    data class Success(val output: String) : AIBuilderUiState()
    data class Error(val message: String) : AIBuilderUiState()
}
